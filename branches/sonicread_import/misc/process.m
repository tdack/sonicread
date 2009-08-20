fs = 44.1e3; polarf = 8180; band = 2e3; % [Hz]
data = wavread('sampledata/polar_s510_2009081601.wav');
[num,den] = butter(2, 2*pi*[polarf-band/2 polarf+band/2], 'bandpass', 's');
Z = tf(num,den);
Zd = c2d(Z,1/fs, 'tustin');
bode(Z, Zd); legend('cont', 'disc');
[numd, dend] = tfdata(Zd);
zdata = filter(numd{:}, dend{:}, data);
[pxx,f] = pwelch(data, [], [], 4*1024, fs);
[zpxx,f] = pwelch(zdata, [], [], 4*1024, fs);
loglog(f, pxx, f, zpxx);
figure; plot(data); hold on; plot(zdata, 'r'); hold off;
zdata = .99*zdata ./ max(zdata); % normalize
wavwrite(zdata, fs, 'sampledata/polar_s510_2009081601_filtered.wav');
