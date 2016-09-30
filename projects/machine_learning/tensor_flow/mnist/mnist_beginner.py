# tensorflow mnist beginner tutorial
# https://www.tensorflow.org/versions/r0.10/tutorials/mnist/beginners/index.html

import tensorflow as tf

# Download the input data. This will give three data sets:
# mnist.train, mnist.test and mnist.validation
from tensorflow.examples.tutorials.mnist import input_data
mnist = input_data.read_data_sets("MNIST_data/", one_hot=True)


# A placeholder for mnist images (these are dim N=784 )
x = tf.placeholder(tf.float32, [None, 784]) # (None means dimension of any length)

# Weights and biasses represented by 'variables'
W = tf.Variable(tf.zeros([784, 10])) # The one-hot images (784 pixels and a one-hot array giving the number)
b = tf.Variable(tf.zeros([10])) # Beta values

# Regression model:
# Multiply multiply x by W then add b, and run through softmax


# Define our cost function, 'cross entrophy' in this case
y_ = tf.placeholder(tf.float32, [None, 10]) # A placeholder to store the correct answers
cross_entropy = tf.reduce_mean(-tf.reduce_sum(y_ * tf.log(y), reduction_indices=[1]))

# Will train the model using gradient descent
train_step = tf.train.GradientDescentOptimizer(0.5).minimize(cross_entropy)

# Initialise the variables
init = tf.initialize_all_variables()
sess = tf.Session()
sess.run(init)


# RUN!
for i in range(1000):
    # Collet a batch of 100 training data points
    batch_xs, batch_ys = mnist.train.next_batch(100)
    # Feed the batch into the session, replacing the placeholders
    sess.run(train_step, feed_dict={x: batch_xs, y_: batch_ys})
    
    
    
# Evaulte the model

# argmax gives the index with the largest value; here compare the most likely image to the correct one and
# see how many times they are equal
correct_prediction = tf.equal(tf.argmax(y,1), tf.argmax(y_,1))