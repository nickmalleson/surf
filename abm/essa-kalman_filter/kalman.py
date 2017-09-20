 # -*- coding: utf-8 -*-
"""
Created on Fri Dec 16 13:55:06 2016

@author: Alice
"""

# Note: will need packages 'pandas' and 'pillow'


# Move to a directory on Nick's laptop
from os.path import expanduser
home = expanduser("~")
import os
os.chdir(home+'/research_not_syncd/git_projects/surf/abm/dda-mesa/alice_dda_code/')

import numpy as np
import workingcameras as cam




np.random.seed(3)

#%%

''' First we generate the true data. We use 7200 minutes for no real reason, 
and a bleed out rate drawn from a normal distribution of 0.5, standard deviation 0.1. 
For now start with 600 agents'''

bleedoutrate_true = np.random.normal(0.5, scale=0.1)

truth = cam.runProgramTrue(bleedoutrate_true, 7200, 600) 


#%%

'''We'll be working with an ensemble of 30 for speed, but ultimately maybe 100
like in the RSO paper.'''

'''We initialise the ensemble drawing the bleed out rate from the prior normal distribution,
mean 0.5 and SD 0.1'''

initial = []

for i in range(30):
    bleedoutrate = np.random.normal(0.5, scale=0.1)
    result = cam.runProgram(bleedoutrate, 61, 600)
    initial.append(result)

initial = np.array(initial)


#%%

''' To look at the spread in bleedoutrates'''

initial[:, -3] 


#%%

''' Now we generate these predictions forward an hour '''

forecasts = []

for i in range(30):
    prediction = cam.runForecast(61, 600, initial[i], 0, 0, 0, steps=60)
    forecasts.append(prediction)
    
forecasts = np.array(forecasts)




#%%

''' We now generate the forecast covariance matrix.
    The covariance matrix is an N x N matrix, where:
    N = M agents locations and route info + bleedoutrate + c_a counts + c_b counts,
    i.e N = 2M + 3'''

''' We find the mean of the forecast ensemble, and find the error from the mean
    for each value '''

means = np.mean(forecasts, axis=0)

adjusted = forecasts - means

covariance = np.cov(adjusted.T)

#%%

''' The covariance[-1][-3] measures how the change in bleed out rate affects
the change in the c_b counts '''

print(covariance[-1][-3])

#%%

''' Begin the data assimilation step '''


#%%

''' First we extract the next observation from the true data '''

observation = [truth[-2][2], truth[-1][2]]

''' Then we create the virtual observations by assuming additive Gaussian noise '''

virtualobs = np.zeros((30,2))

for i in range(30):
    for j in range(2):
        virtualobs[i][j] = observation[j] + np.random.normal(0, 15)


#%%

''' Code the matrix H, the forward model, which is just a transformation matrix
changing the state vector into the same form as the observation vector '''

H = np.zeros((2, 2*600 + 3))

H[-1][-1] = 1
H[0][-2] = 1


#%%

''' Calculate the Kalman gain matrix '''
'''R contains the variance of the random errors (i.e 15^2) '''

P = covariance
R = np.array([[225, 0],[0, 225]])

# tbi = 'to be inverted'
tbi = np.dot(np.dot(H,P),H.T) + R

''' We want to solve K tbi = P H.T to find K 
    rewrite to form tbi.T K.T = H P.T'''

LHS = tbi.T
RHS = np.dot(H,P.T)

Ktranspose = np.linalg.lstsq(LHS,RHS)

K = Ktranspose[0].T


#%%

''' Create the ensemble analysis '''

ens_analysis = []

for i in range(30):
    tbm = virtualobs[i] - np.dot(H,forecasts[i])
    adjust = forecasts[i] + np.dot(K,tbm)
    ens_analysis.append(adjust)



#%%

''' Average the ensemble analysis, find analysis covariance '''

ens_means = np.mean(ens_analysis, axis=0)

ens_error = ens_analysis - ens_means

ens_covariance = np.cov(ens_error.T)


#%%

'''-----------------------------------------------------------------------------'''
'''                   BEGINNING OF THE SAME THING, LOOPED...                    '''
'''-----------------------------------------------------------------------------'''


stored_forecast = []
stored_forecast_uncertainty = []
stored_analysis = []
stored_analysis_uncertainty = []
stored_truth = []
stored_parameter = []

forecast_error = []
analysis_error = []
observation_error = []


#%%

for n in range(3,120):

    print(n)

    forecasts = []
    #previously started at time T=241
    for i in range(30):
        prediction = cam.runForecast((((n-1)*60)+1), 600, ens_analysis[i], 0, 0, 0, steps=60)
        forecasts.append(prediction)
    
    forecasts = np.array(forecasts)


    means = np.mean(forecasts, axis=0)
    
    adjusted = forecasts - means

    covariance = np.cov(adjusted.T)




    ''' Begin the data assimilation step '''




    ''' First we extract the next observation from the true data '''

    observation = [truth[-2][n], truth[-1][n]]

    ''' Then we create the virtual observations by assuming additive Gaussian noise '''

    virtualobs = np.zeros((30,2))

    for i in range(30):
        for j in range(2):
            virtualobs[i][j] = observation[j] + np.random.normal(0, 15)

    meansobs = np.mean(virtualobs, axis=0)
    virtualavg = meansobs[-1]

    ''' Code the matrix H, the forward model, which is just a transformation matrix
    changing the state vector into the same form as the observation vector '''

    H = np.zeros((2, 2*600 + 3))

    H[-1][-1] = 1
    H[0][-2] = 1




    ''' Calculate the Kalman gain matrix '''

    P = covariance
    R = np.array([[225, 0],[0, 225]])

    # tbi = 'to be inverted'
    tbi = np.dot(np.dot(H,P),H.T) + R

    ''' We want to solve K tbi = P H.T = RHS to find K 
    rewrite to form tbi.T K.T = H P.T'''

    LHS = tbi.T
    RHS = np.dot(H,P.T)

    Ktranspose = np.linalg.lstsq(LHS,RHS)

    K = Ktranspose[0].T




    ''' Create the ensemble analysis '''

    ens_analysis = []

    for i in range(30):
        tbm = virtualobs[i] - np.dot(H,forecasts[i])
        adjust = forecasts[i] + np.dot(K,tbm)
        ens_analysis.append(adjust)





    ''' Average the ensemble analysis, find analysis covariance '''

    ens_means = np.mean(ens_analysis, axis=0)

    ens_error = ens_analysis - ens_means

    ens_covariance = np.cov(ens_error.T)



 

    ''' Store the results we ultimately want to plot '''

    stored_forecast.append(means[-1])
    stored_forecast_uncertainty.append(covariance[-1][-1])
    stored_analysis.append(ens_means[-1])
    stored_analysis_uncertainty.append(ens_covariance[-1][-1])
    stored_truth.append(virtualavg)
    stored_parameter.append(ens_means[-3])
    
    
    forecast_error.append(means[-1] - observation[-1])
    analysis_error.append(ens_means[-1] - observation[-1])
    observation_error.append(virtualavg - observation[-1])

#%%

smallertruth = truth[-1][3:120]



#%%

import matplotlib.pyplot as plt
xaxis = [i for i in range(117)]

plt.plot(xaxis[0:4], stored_forecast[0:4], xaxis[0:4], stored_analysis[0:4])


#%%

m = 0
n = 117


fig = plt.figure(figsize=(14,20))

ax = plt.subplot(3,1,1)

plt.plot(stored_forecast[m:n], label="forecast")

plt.plot(stored_analysis[m:n], label="analysis")

plt.plot(stored_truth[m:n], label="virtual observations")

plt.plot(smallertruth[m:n], label="truth")

ax.set_xlabel('Hour')
ax.set_ylabel('Count of agents')

plt.legend(bbox_to_anchor=(1.05, 1), loc=2, borderaxespad=0.)


m = 91
n = 96

xaxis = [i for i in range(m,n)]

ax = plt.subplot(3,1,2)

plt.plot(xaxis, stored_forecast[m:n], label="forecast")

plt.plot(xaxis, stored_analysis[m:n], label="analysis")

plt.plot(xaxis, stored_truth[m:n], label="virtual obs")

plt.plot(xaxis, smallertruth[m:n], label="truth")

plt.legend(bbox_to_anchor=(1.05, 1), loc=2, borderaxespad=0.)

ax.set_xlabel('Hour')
ax.set_ylabel('Count of agents')


ax = plt.subplot(3,1,3)

plt.plot(stored_parameter[0:117], label="'True' bleedout rate")

plt.plot([bleedoutrate_true for i in range(117)], label="Estimated bleedout rate")

plt.legend(bbox_to_anchor=(1.05, 1), loc=2, borderaxespad=0.)


ax.set_xlabel('Hour')
ax.set_ylabel('Parameter value')

plt.show()





#%%

from numpy import mean, sqrt, square

forecast_RMSE = sqrt(mean(square(forecast_error)))
analysis_RMSE = sqrt(mean(square(analysis_error)))
observation_RMSE = sqrt(mean(square(observation_error)))


#%%


fig.savefig('image' + str(bleedoutrate_true) + ' ' + str(forecast_RMSE) \
         + ' ' + str(analysis_RMSE) + ' ' + str(observation_RMSE) + ' ' + '10' + '.png')