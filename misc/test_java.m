% x = matlab output
% y = java putput

clear sl;
addpath scripts;
% if exist('SonicLink') ~= 8
    javaclasspath('/home/remco/dev/polar/SonicRead/build/classes/');
    import sonicread.*
    clear java;
% end

sl = SonicLink;
hsr = CreateHsr;

Fs = 44100;
% load polar_f6_start.mat;
x0 = read_wav_fs('~/dev/polar/SonicRead/misc/sampledata/polar_f6.wav', Fs);
% x0 = read_wav_fs('~/dev/polar/SonicRead/misc/sampledata/polar_s510_20090119_2.wav', Fs);
% x0 = read_wav_fs('/tmp/test.wav', Fs);
% x0 = x0(1, 1:15e3);
    if size(x0,1) > 1,
        x0 = x0(2,:); % only use the second channel for ubuntu on nb70541!
    else
        x0 = x0(1,:);
    end

if 1,
hsrs = [];
sl.restart();
for ii = 1:size(x0,2)
	val = sl.decode(x0(ii));
    if(val >= 0)
        hsrs = [ hsrs val ];
        hsr.AddData(val);
    end
end
for val = hsrs,
    hsr.AddData(val);
end
return;
end

hsr = [];
x1 = zeros(size(x0));
x2 = x1; x3 = x1; x4 = x1;
dec = zeros(size(x0,2), 7);
timeIndex = 0;tActive = 0; c=zeros(1,6); badBytes = 0;
sl.restart();
for ii = 1:size(x0,2)
    timeIndex = timeIndex + 1;
    x1(ii) = sl.filter(x0(ii));
    x1(ii) = sl.dilate(x1(ii));
    x1(ii) = sl.median(x1(ii));
    x2(ii) = sl.make_decision(x1(ii));
    % decisions[0..4] = above/below threshold for five different threshold levels
    % decisions[5] = best above/below decision based on a majority rule
    % decisions[6] = abrupt change/no abrupt change in the amplitude (start flag)
    dec(ii,:) = sl.decisions;
    sl.b_median(); % apply filter to decisions
    x(4) = 0;
    
    % activation test
    if((sl.decisions(6) > 0) && (tActive == 0))
      tActive = timeIndex;
      t_byte = 0;
      n_bytes = 0;
      fprintf(1, 'Processing signal at %d\n', timeIndex);
    end
    if tActive == 0,
        continue;
    end
    if(timeIndex > tActive + 10000)
      fprintf(1, 'byte start time out\n');
      break;
    end
    
    % new byte?
    if(t_byte == 0 && (sl.decisions(5) > 0)) 
      fprintf(1, 'New byte at %d\n', timeIndex);
      tActive = timeIndex;
      t_byte = timeIndex;
      n_bytes = n_bytes + 1;
      n_byte = 0;
      for i = 1:6,
    	c(i) = 0;
      end
    end
    
    % processing a byte?
    if(t_byte > 0)
      j = int32(floor((timeIndex - t_byte) / 88));  % bit number; the duration of a bit is very close t
      
      % end of the byte
      if(j >= 10)
        if((n_byte & 1) == 0) % first bit (lsb) must be a one
    	  error('Bad byte start %d', n_byte);
        end
    	if(n_byte >= 512) % last bit (msb) must be a zero
          error('Bad byte finish. Restarting..');
        end
    	n_byte = bitshift(n_byte, -1);
        n_byte = mod(n_byte, 256);
    	t_byte = 0;
        if(n_bytes > 5) % got all sync bytes
    	  %return n_byte;
          fprintf(1, 'Got byte %X\n', n_byte);
          hsr = [ hsr n_byte ];
          continue;
        end
    	if(n_byte ~= hex2dec('AA')) % bad synchronization byte
          badBytes = badBytes + 1;
          fprintf(sprintf('Bad synchronization byte %X (%d in total).\n', n_byte, badBytes));
        end
        fprintf(1, 'Got sync byte #%d\n', n_bytes);
        continue; % discard sync byte
      end
      
      % sample number inside the bit
      k = int32(timeIndex - t_byte - (88 * j));      
%       fprintf(1, 'ti %.1f | tb %.1f | ti - tb %d | j %d | k %d\n', timeIndex, t_byte, timeIndex - t_byte, j, k);
%       if timeIndex - t_byte > 100
%           return;
%       end

      if(k < 64) % the bit burst has a duration of close to 60 samples (here we u
        % count number of positive hits
        c = c + double(sl.decisions(1:6))';
        x4(timeIndex) = 1;
      else
        if(k == 64)
          k = 0;
%           fprintf(1, '%2d ', c);
          for i = 1:6,
            if(c(i) >= 30) % threshold = 60/2
                if(i < 5)
                    k = k + 1;
                else
                    k = k + 2;
                end
            end
           c(i) = 0;
          end
          if(k >= 4) % majority rule
            n_byte = n_byte + bitshift(1,double(j));
          end
          fprintf(1, '| %d %d\n', k, k >= 4);
%           break;
        end
      end  
    end
end
hsr

figure(2);
plot(x0, 'g.-')
hold on;
plot(x1, 'b');
plot(x2, 'k');
plot(.2*sum(dec(:,1:4),2), 'r'); % display majority rule
plot(.25*x4, 'k');
hold off;
ylim([-.1 1]);
xlim([4000 5500]);
xlim([15.3e3 16.5e3]);
grid on;