# tensorflow mnist beginner tutorial
# https://www.tensorflow.org/versions/r0.10/tutorials/mnist/beginners/index.html

# Download the input data. This will give three data sets:
# mnist.train, mnist.test and mnist.validation
from tensorflow.examples.tutorials.mnist import input_data
mnist = input_data.read_data_sets("MNIST_data/", one_hot=True)


