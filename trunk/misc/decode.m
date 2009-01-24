function s410_decode(command);
% Tom�s Oliveira e Silva
%                                                                                                  
%Here goes the matlab code. It was written in 2001, so I don't recall                              
%well what I did. I remember well, though, the following:                                          
%
%a) each bit of each byte is encoded by the presence (or absence) of                               
%a sinusoidal, with a frequency above 4000Hz (a spectral analysis                               
%should give you a more accurate value)                                                         
%
%b) each byte has one start bit and one stop bit, i.e., for each byte                              
%10 bits are transmitted                                                                        
%)
%c) there is a short (variable?) silence between the bytes                                         
%
%d) the bytes are organized in sections                                                            
%
%e) there is a larger (variable) silence between the sections                                      
%
%f) each section is terminated by two bytes (crc16 cyclic redundancy                               
%check) used for error detection                                                                
%
%Look at the C code to see how f) can be dealt with. The rest, hopefully,                          
%should be possible to observe using the matlab code.                                              
%

global U x0 x1 x2 x3 x4 B
if nargin < 1
  command = -1; % init
end

switch command
  case -1,
    clc;
    close all;
    % read signal
%     [x0,Fs] = wavread('~/dev/polar/data/hardlopen_20080529.wav');
%     [x0,Fs] = wavread('~/dev/polar/data/fietsen_20080731.wav');
%         [x0,Fs] = wavread('/tmp/1.wav');
%         [x0,Fs] = wavread('~/Desktop/SonicLink.wav');
       [x0,Fs] = wavread('~/dev/polar/SonicRead/misc/sampledata_20090119.wav');
       [x0,Fs] = wavread('~/dev/polar/SonicRead/misc/sampledata_polar_f6.wav');
%     [x0,Fs] = wavread('~/dev/polar/data/test.wav');
%     [foobar, chan_highest_gain] = max(std(x1));
%     chan_highest_gain
%     x1 = x1(:,chan_highest_gain);
    x0 = x0';
    % h = fopen('../data/08081701_wav.dat', 'w+'); for i=1:size(x0,2), fprintf(h, '%e\n', x0(2,i)); end; fclose(h);
    
%     % remove some samples for 20080731
%     x0(:,1:4300) = [];
%     x0(:,12000:end) = [];
%   

    if size(x0,1) > 1,
        x1 = x0(2,:); % only use the second channel for ubuntu on nb70541!
    else
        x1 = x0(1,:);
    end
    
%     x1(1,:) = x1(2,:);
    fprintf(1,'Fs=%.0fHz\n',Fs);
    fprintf(1,'%d samples\n',length(x1));
    
    % convert to required sampling frequency
    Fsi = 44100;    
    if Fsi ~= Fs,
        fprintf(1, 'Converting from %.1fkHz to %.1fkHz\n', Fs*1e-3, Fsi*1e-3);
        
        t = [1:size(x1,2)]./Fs;
        ti = [1:size(x1,2)/(Fs/Fsi)]./Fsi;
        xi = interp1(t,x1,ti);
        
        if 0,
            plot(t, x1, '.-');
            hold on;
            plot(ti, xi, 'r.-');
            hold off;
        end
        
        Fs = Fsi;
        x1 = xi;
        x0 = xi;
        
    end
    
    % remove low frequency components and rectify signal
    h = remez(40,[0 2000 4000 22050]/22050,[0 0 1 1],[2 1]);
    %h = [ 0.00379802504960  -0.01015567605831  -0.00799539667861  -0.00753846964193
    %     -0.00613119820747  -0.00283788804837   0.00237999784896   0.00894445674876
    %      0.01562664364293   0.02085836025169   0.02288210087128   0.02006200096880
    %      0.01129559749322  -0.00374264061795  -0.02440163523552  -0.04905850773028
    %     -0.07526471722031  -0.10006201874319  -0.12045502068751  -0.13386410688608
    %      0.86146086608477  -0.13386410688608  -0.12045502068751  -0.10006201874319
    %     -0.07526471722031  -0.04905850773028  -0.02440163523552  -0.00374264061795
    %      0.01129559749322   0.02006200096880   0.02288210087128   0.02085836025169
    %      0.01562664364293   0.00894445674876   0.00237999784896  -0.00283788804837
    %     -0.00613119820747  -0.00753846964193  -0.00799539667861  -0.01015567605831
    %      0.00379802504960 ]%
    x1 = abs(filter(h,1,x1));
    % dilate and filter signal
    
    x1 = ordfilt2(x1,7,ones(1,7));
    x1 = ordfilt2(x1,3,ones(1,5));
    % compute detection threshold (peak detection with decay)
    x2 = x1;
    for k=2:length(x2)
      % ewma
      x2(k)=max(x2(k),0.9999*x2(k-1));
%       x2(k)=max(x2(k),.99999*x2(k-1));
    end
    % x0(2,:) = x2;
    % dilate threshold
    x2 = ordfilt2(x2,41,ones(1,41));
    % detect start of transmission (last part is significantly higher than
    % the fist part)
    h = min(find(x2(1+30:length(x2)) > 25*x2(1:length(x2)-30)));
    if length(h)==0
      error('no transmission detected');
    end
    h = h + 30;
    fprintf(1,'start at %d\n',h);
    % apply threshold
    x3 = (x1 >= 0.35*x2);
    x3(1:h) = zeros(1,h);
    x3 = ordfilt2(x3,3,ones(1,5));
    % compute byte frames
    x4 = zeros(1,length(x3));
    I = 1+find(x3(1:length(x3)-1) ~= x3(2:length(x3)));
%     whos
%     error('ok');
    k = 1;
    while 1
      h = I(k);
      x4(h:h+881)=ones(1,882);
      while k<length(I) & I(k)<h+900
        k = k+2;
      end
      if k > length(I)
        break;
      end
    end
    % decode bytes
    Is = 1+find(x4(1:length(x4)-1) ~= x4(2:length(x4)));
    Ie = Is(2:2:length(Is))-1;
    Is = Is(1:2:length(Is));
%     whos
    length(Is)
    for k=1:length(Is)
      c = 0;
      for l=0:9
        b = sum(x3(Is(k)+88*l+1:Is(k)+88*l+70));
        if l == 0 & b < 35
          error('bad syncronization bit');
        end
        if l == 9 & b > 25
          error('bad syncronization bit');
        end
        if l > 0 & l < 9
          if b > 25 & b < 35
            error('possibly incorrect bit');
          end
          b = b > 30;
          c = c + b * 2^(l-1);
          fprintf(1,'%d',b);
        end
      end
      if k < length(Is)
        g = (Is(k+1)-Ie(k))/88.2;
      else
        g = 0;
      end
      B(k) = c;
      fprintf(1,'  %3d %02X [%6.3f]\n',c,c,g);
    end
    % user interface
    figure(1);
    set(1,'Position',[12 288 1000 420]);
    U(1) = uicontrol('Callback','s410_decode(1)',...
                     'Style','slider',...
                     'Units','normalized',...
                     'Position',[0.01 0.01 0.98 0.04],...
                     'Visible','on',...
                     'Min',1,...
                     'Max',max(1,length(x1)-10000),...
                     'SliderStep',7500/(length(x1)-10000)*[0.999 1.001],...
                     'Value',1);
    U(2) = uicontrol('Callback','s410_decode(2)',...
                     'Style','pushbutton',...
                     'Units','normalized',...
                     'Position',[0.01 0.92 0.06 0.05],...
                     'String','hide x1');
    U(3) = uicontrol('Callback','s410_decode(3)',...
                     'Style','pushbutton',...
                     'Units','normalized',...
                     'Position',[0.01 0.86 0.06 0.05],...
                     'String','hide x2');
    U(4) = uicontrol('Callback','s410_decode(4)',...
                     'Style','pushbutton',...
                     'Units','normalized',...
                     'Position',[0.01 0.80 0.06 0.05],...
                     'String','hide x3');
    U(5) = uicontrol('Callback','s410_decode(5)',...
                     'Style','pushbutton',...
                     'Units','normalized',...
                     'Position',[0.01 0.74 0.06 0.05],...
                     'String','hide x4');
    command = 0;
  case 1,
    command = 0;
  case 2,
    if strcmp('show x1',get(U(2),'String'))
       set(U(2),'String','hide x1');
    else
       set(U(2),'String','show x1');
    end
    command = 0;
  case 3,
    if strcmp('show x2',get(U(3),'String'))
       set(U(3),'String','hide x2');
    else
       set(U(3),'String','show x2');
    end
    command = 0;
  case 4,
    if strcmp('show x3',get(U(4),'String'))
       set(U(4),'String','hide x3');
    else
       set(U(4),'String','show x3');
    end
    command = 0;
  case 5,
    if strcmp('show x4',get(U(5),'String'))
       set(U(5),'String','hide x4');
    else
       set(U(5),'String','show x4');
    end
    command = 0;
end

if command == 0
  offset = floor(get(U(1),'Value'));
%   offset = 1;
  I = offset:min(offset+10000,length(x1));
  plot(I-I(1),x0(I)./max(max(x0)), 'g');
  hold on;
  if strcmp('hide x1',get(U(2),'String'))
    plot(I-I(1),x1(I),'b');
    hold on;
  end
  if strcmp('hide x2',get(U(3),'String'))
    plot(I-I(1),x2(I),'k');
    hold on;
  end
  if strcmp('hide x3',get(U(4),'String'))
    plot(I-I(1),0.09*max(x2(I)).*x3(I),'r');
    hold on;
  end
  if strcmp('hide x4',get(U(5),'String'))
    plot(I-I(1),0.10*max(x2(I)).*x4(I),'k');
    hold on;
  end
  hold off;
  zoom on; grid on;
  t = sprintf('offset = %d',offset);
  ylim([0 max(max(x2))]);
  title(t);
  drawnow;
end
