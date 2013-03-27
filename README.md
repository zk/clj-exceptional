# clj-exceptional

A Clojure client for [Exceptional](http://getexceptional.com).

[![Build Status](https://travis-ci.org/zkim/clj-exceptional.png)](https://travis-ci.org/zkim/clj-exceptional)

## Usage

### Lein / Cake

```clojure
[clj-exceptional "0.8.0"]
```

### Importing

```clojure
(ns foo
  (:require [clj-exceptional :as cx]))

;; or

(ns foo
  (:use cljs-exceptional))
```

For the examples below I'll assume you're using the require method above.

### Basic Usage

Set your key !! IMPORTANT !!:

```clojure
(cx/key! "exceptional_app_api_key")
```

Post an exception (blocking):

```clojure
(cx/post-exc (Exception. "something went wrong!"))
```

Post an exception (non-blocking using agents):

```clojure
(cx/post-exc-async (Exception. "something went wrong!"))
```



### "Catch" Macros

Wrap some code (`catch-exc` returns the caught exception):

```clojure
(cx/catch-exc
  (op-throw-exception))

;; => #<Exception>
```

Wrap some code (`rethrow-exc` re-throws the caught exception):

```clojure
(try
  (cx/rethrow-exc
    (op-throws-exception))
  (catch Exception e (println "Something went wrong!!!")))

;; => nil
```
### Ring Handler

clj-exceptional contains ring handlers (`wrap-exceptional-catch` and
`wrap-exceptional-rethrow`) that will add information from
the request map to the post.

```clojure
(def ring-app
  (-> routes
      (wrap-params)
      (wrap-file "resources/public")
      (wrap-file-info)
      (cx/wrap-exceptional-catch))
```

This will send an exceptional post containing request parameters such
as `:remote-addr`, `:uri`, and `:headers`.


### Contributing

Open up an issue or send me a pull request.


### TODO

* Expose more of ring request map entries, such as `:scheme`,
  `:content-length`, and `:port`.



## License

Copyright (C) 2010 Zachary Kim

Distributed under the Eclipse Public License, the same as Clojure.
