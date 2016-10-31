# Theano tutorial 
# http://deeplearning.net/software/theano/tutorial/


import numpy
import theano.tensor as T
from theano import function
from theano import pp # For pretty printing

# ************************************
# Adding two scalars
# ************************************

x = T.dscalar('x') # Create a TensorVariable of scanar type (name 'x' is not required, but helps with debugging)
y = T.dscalar('y') # dscalar is for  “0-dimensional arrays (scalar) of doubles (d)”. Could also use (e.g.) T.dmatrix
z = x + y # z is another Variable
print pp(z) # Verify what z does by pretty printing

# Create a 'Function' (a theano class) to add two numbers. 
# First argument will be list of variables (inputs), second argument is what we see as output
# when the function is applied (?)
f = function([x, y], z) 


# Use f() to add two numbers
f(2,3)


# ************************************
# Exercise
# ************************************

a = T.vector() # declare variable
out = a + a ** 10               # build symbolic expression
f = function([a], out)   # compile function
print(f([0, 1, 2]))


# Modify to calculate experssion: a ** 2 + b ** 2 + 2 * a * b.

b = T.vector()
out2 = a ** 2 + b ** 2 + 2 * a * b
f = function([a, b], out2)   # compile function
print(f([0, 1, 2], [3,4,5]))




# ************************************
# Other examples
# http://deeplearning.net/software/theano/tutorial/examples.html
# ************************************

# Logistic function

x = T.dmatrix('x')
s = 1 / (1 + T.exp(-x))
logistic = function([x], s)
logistic([[0, 1], [-1, -2]])


# Functions with multiple outputs

a, b = T.dmatrices('a', 'b') # For convenience, 'dmatrices' produces as many matrices as names it is given
diff = a - b
abs_diff = abs(diff)
diff_squared = diff**2
f = function([a, b], [diff, abs_diff, diff_squared])
f([[1, 1], [1, 1]], [[0, 1], [2, 3]])



# Functions with default values

from theano import In
x, y = T.dscalars('x', 'y')
z = x + y
f = function([x, In(y, value=1)], z)
f(33)
f(33, 2)



# Shared values (i.e. internal state)

from theano import shared
state = shared(0) # A value that can be shared by many functions. Can be used in expressions like other TensorVariables
inc = T.iscalar('inc')
accumulator = function([inc], state, updates=[(state, state+inc)])



