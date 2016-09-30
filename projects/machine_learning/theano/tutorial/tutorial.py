# Theano tutorial 

# Adding two scalars
import numpy
import theano.tensor as T
from theano import function
from theano import pp # For pretty pringing


x = T.dscalar('x') # Create a TensorVariable of scanar type
y = T.dscalar('y') # (name 'x' is not required, but helps with debugging)
z = x + y
print pp(z) # Verify what z does by pretty printing

# Function to add two numbers. First argument will be list of variables (inputs), second 
# argument is 
f = function([x, y], z) 


# Use f() to add two numbers
f(2,3)