(ns test-clj-exceptional
  (:use clj-exceptional :reload)
  (:use clojure.test))

(deftest test-appenv
  (let [res (:application_environment
             (appenv {}
                     (framework "rails")
                     (env {"foo" "bar"})
                     (language "ruby")
                     (language-version "1.8.7")
                     (root-dir "/foo/bar")))]
    (are [v k] (= v (k res))
         "rails"          :framework
         {"foo" "bar"}    :env
         "ruby"           :language
         "1.8.7"          :language_version
         "/foo/bar"       :application_root_directory)))


(deftest test-exception
  (let [res (:exception
             (exception {}
                        (occured "now")
                        (message "foo")
                        (backtrace ["foo" "bar"])
                        (exc-cls "some.Class")))]
    (are [v k] (= v (k res))
         "now"         :occured_at
         "foo"         :message
         ["foo" "bar"] :backtrace
         "some.Class"  :exception_class)))


(deftest test-request
  (let [res (:request
             (request {}
                      (session ["key" "val"])
                      (remote-ip "1234")
                      (params {"foo" "bar"})
                      (action "index")
                      (url "/index")
                      (req-method "GET")
                      (controller "main")
                      (headers {"baz" "bap"})))]
    (are [v k] (= v (k res))
         ["key" "val"] :session
         "1234"        :remote_ip
         {"foo" "bar"} :parameters
         "index"       :action
         "/index"      :url
         "GET"         :request_method
         "main"        :controller
         {"baz" "bap"} :headers)))

(deftest test-format-exception
  (let [res (format-exception (Exception. "test exception"))]
    (is (:application_environment res))
    (is (:exception res))
    (is (:client res))))

(def ex-req-map {:remote-addr "0:0:0:0:0:0:0:1%0",
                 :scheme :http,
                 :query-params {},
                 :session {"key" "val"},
                 :form-params {},
                 :multipart-params {},
                 :request-method :get,
                 :query-string nil,
                 :content-type nil,
                 :cookies
                 {"ring-session" {:value "3862a562-7d4d-448f-92d3-59f786023fa4"},
                  "remember_user_token"
                  {:value
                   "BAhbB2kGSSIZdVlrUGtBY0tuLVk5TGc2UkJRMF8GOgZFVA==--68edd1936590b325f476c5519198cee28afbb336"}},
                 :uri "/foobar",
                 :server-name "localhost",
                 :params {:foo "bar" :baz "bap"},
                 :headers
                 {"hkey" "hval"},
                 :content-length nil,
                 :server-port 8080,
                 :character-encoding nil})

(deftest test-add-request
  (let [res (:request (add-request {}  ex-req-map))]
    (are [v k] (= v (k res))
         {"key" "val"}           :session
         "0:0:0:0:0:0:0:1%0"     :remote_ip
         {:foo "bar" :baz "bap"} :parameters
         "/foobar"               :url
         {"hkey" "hval"}         :headers)))



