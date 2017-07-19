# Order book, Limit and Iceberg orders

### Overview

java 8 (compilation with `-parameters` is required), lombok, gradle

### Complexity

`p` - number of transactions generated; `n` - number of different prices in order book

adding new order: `O(p * log(n))`

### Coverage

all except `main` method