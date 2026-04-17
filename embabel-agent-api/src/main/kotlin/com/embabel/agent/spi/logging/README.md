# Logging

All logging in implementation classes is at `debug` level by default. Logging that appears at `info` level and above
comes from an event listener that logs.

Thus it's easy to change logging personality and anything that appears in logs can be subscribed to in an event
listener.