# game-of-life

Yet another conway's game

## [Play with online demo!](https://codepen.io/Ivana-/full/YoXgQm)

![alt text](https://user-images.githubusercontent.com/10473034/59426410-cba09400-8de0-11e9-96d9-6352fab36ac7.png "Screenshot")

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

## License

Copyright © 2019

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
