# JPEG-2000 Image I/O
----------------
## Features
- Implemented 1D-DCT, 1D-FFT and the corresponding fast versions,
- Implemented 2D version FFT and DCT,
- Attempted to read/write jpeg file head from scratch(byte information) without using any outside JAVA standard library, 
- Found interesting secrets in jpeg file decoding that jpeg file is not encoded/decoded according to its head information(DHT/DQT sections, etc.). They are encoded and decoded by a commonly adopted version of coding.

## Usage
**This is just a personal side project.**
- Main class includes test function for each feature.
- See below for more details.
![Image of Functions](https://github.com/drmeerkat/JPEG-2000-Image-IO/blob/master/Picture1.png)
