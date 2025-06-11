import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense

# Example: input shape (samples, timesteps, features)
# Say we have 100 samples, each with 10 timesteps and 1 feature
X = np.random.rand(100, 10, 1)
y = np.random.rand(100, 1)

# Build the LSTM model
model = Sequential([
    LSTM(64, input_shape=(10, 1)),  # 64 units, input: 10 timesteps with 1 feature
    Dense(1)  # Output layer
])

model.compile(optimizer='adam', loss='mse')

# Train the model
model.fit(X, y, epochs=10, batch_size=16)
