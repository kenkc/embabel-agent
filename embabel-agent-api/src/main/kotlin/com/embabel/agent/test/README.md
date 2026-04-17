# Testing support classes

Enabling testing is critical for any programming model.

This package contains classes useful for both unit and integration testing.

`unitTestUtils` methods help unit test `@Agent` classes calling LLMs.
`integrationTestUtils` methods help integration test agents, with a default LLM that creates valid instances of return
types with random values.