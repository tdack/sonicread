fs = 44.1e3; polarf = 8180; band = 1e3; % [Hz]
data = wavread('sampledata/polar_s510_2009081601.wav');
data = data ./ max(data); % normalize
wavwrite(data, fs, 'sampledata/polar_s510_2009081601_normalized.wav');
[num,den] = butter(5, 2*pi*[polarf-band/2 polarf+band/2], 'bandpass', 's');
[num,den] = butter(2, 2*pi*[polarf-band/2 polarf+band/2], 'bandpass', 's');
Z = tf(num,den);
% Zd = c2d(Z,1/fs, 'tustin');
Zd = c2d(Z,1/fs, 'prewarp', 2*pi*polarf);
figure(1); bode(Z, Zd); legend('cont', 'disc');
[numd, dend] = tfdata(Zd, 'v');
zdata = filter(numd, dend, data);
zdata = .99*zdata ./ max(zdata); % normalize
[pxx,f] = pwelch(data, [], [], 4*1024, fs);
[zpxx,f] = pwelch(zdata, [], [], 4*1024, fs);
figure(2); loglog(f, pxx, f, zpxx);
figure(3); plot([1:size(data,1)]/fs, data); hold on; plot([1:size(data,1)]/fs, zdata, 'r'); hold off; xlim([5.015 5.04]);
wavwrite(zdata, fs, 'sampledata/polar_s510_2009081601_filtered.wav');

numd_str = strrep(mat2str(numd), ' ', ', '); numd_str = strrep(numd_str, ' 0,', ' 0.0,'); 
dend_str = strrep(mat2str(dend(2:end)), ' ', ', '); dend_str = strrep(dend_str, ' 0,', ' 0.0,'); 
fprintf('Copy into Java code: \n');
fprintf('Double[] numd = { %s };\n', numd_str(2:end-1));
fprintf('Double[] dend = { %s };\n', dend_str(2:end-1));

