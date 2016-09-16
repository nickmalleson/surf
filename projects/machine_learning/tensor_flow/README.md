# Notes about using Tensor Flow

## Install

Follow these to set it all up:

[https://www.tensorflow.org/versions/r0.10/get_started/os_setup.html](https://www.tensorflow.org/versions/r0.10/get_started/os_setup.html)

GPU mac instructions: https://www.tensorflow.org/versions/r0.10/get_started/os_setup.html#optional-setup-gpu-for-mac

Basically:

 - Use the anaconda version: https://github.com/conda-forge/tensorflow-feedstock (this is only CPU, not GPU).

```
conda config --add channels conda-forge
conda install tensorflow
conda create -n tensorflow python=2.7
source activate tensorflow
export TF_BINARY_URL=https://storage.googleapis.com/tensorflow/mac/cpu/tensorflow-0.10.0-py2-none-any.whl
pip install --ignore-installed --upgrade $TF_BINARY_URL
```


