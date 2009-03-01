function x0 = read_wav_fs(fn, Fsi)
% Read a wav file in a defined sampling frequency
%    read_wav_fs(filename, sampling frequency);

    [x0,Fs] = wavread(fn);
%     [x0,Fs] = wavread('~/dev/polar/SonicRead/misc/sampledata/polar_f6.wav');
    x0 = x0';
    
    if Fsi ~= Fs,
        fprintf(1, 'Converting from %.1fkHz to %.1fkHz\n', Fs*1e-3, Fsi*1e-3);
        
        t = [1:size(x0,2)]./Fs;
        ti = [1:size(x0,2)/(Fs/Fsi)]./Fsi;
        xi = interp1(t,x0,ti);
        
        if 0,
            plot(t, x0, '.-');
            hold on;
            plot(ti, x0, 'r.-');
            hold off;
        end
        
        Fs = Fsi;
        x0 = xi;
        
        fprintf(1, 'Done\n');
    end
    wavwrite(x0,Fs,'/tmp/test.wav');
    
return;