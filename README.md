# game-of-life

Yet another conway's game



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
