(ns clj-exceptional
  (:require [cheshire.core :as che]
            [clj-http.client :as client]
            [clj-http.util :as cutil])
  (:import [java.text SimpleDateFormat]))

(def *root-dir* (.getAbsolutePath (java.io.File. "")))
(def date-format "yyyy-MM-dd'T'HH:mm:ssZZZZ")
(def date-formatter (SimpleDateFormat. date-format))
(def _api-key (atom ""))
(def _language (atom "clojure"))
(def _language-version (atom (clojure-version)))

(defn key! [new-key]
  (reset! _api-key new-key))

(defmacro with-key [key & body]
  `(binding [_api-key (atom ~key)]
     ~@body))

(defn format-timestamp [ts]
  (.format date-formatter ts))

(defn now-iso []
  (format-timestamp (System/currentTimeMillis)))

(def client-info
  {:name "clj-exceptional"
   :version "0.7.0"
   :protocol_version "6"})

(def shell-env (reduce
                #(assoc %1 (.getKey %2) (.getValue %2))
                {}
                (System/getenv)))

(def java-env (let [props (System/getProperties)]
                (reduce #(assoc %1 %2 (.getProperty props %2))
                        {}
                        (.stringPropertyNames props))))

;; Threaders

(defmacro defex [name & path]
  `(defn ~name [m# val#]
     (assoc-in m# [~@path] val#)))

;; Require exceptional infos
;; + exception/backtrace
;; + exception/exception_class
;; + exception/message
;; + exception/occured_at x
;; + application_environment/application_root_directory
;; + application_environment/env

(defn clean-stacktrace [^Exception e]
  (->> (.getStackTrace e)
       (seq)
       (map #(.toString %))))

(defmacro appenv [m & forms]
  `(assoc ~m
     :application_environment
     (-> (:application_environment ~m)
         ~@forms)))

(defex framework :framework)
(defex env :env)
(defex language :language)
(defex language-version :language_version)
(defex root-dir :application_root_directory)

(defmacro exception [m & forms]
  `(assoc ~m
     :exception
     (-> (:exception ~m)
         ~@forms)))

(defex occured :occured_at)
(defex message :message)
(defex backtrace :backtrace)
(defex exc-cls :exception_class)

(defmacro request [m & forms]
  `(assoc ~m
     :request
     (-> (:request ~m)
         ~@forms)))

(defex session :session)
(defex remote-ip :remote_ip)
(defex params :parameters)
(defex action :action)
(defex url :url)
(defex req-method :request_method)
(defex controller :controller)
(defex headers :headers)


(defn format-exception
  ([^Exception e]
     (-> {}
         (exception
          (backtrace (clean-stacktrace e))
          (exc-cls (.getCanonicalName (class e)))
          (message (.getMessage e))
          (occured (now-iso)))
         (appenv
          (root-dir *root-dir*)
          (env java-env)
          (language @_language)
          (language-version @_language-version))
         (assoc :client client-info))))

;;
;; Usage
;;

(defn post [exc-map]
  (client/post "http://api.getexceptional.com/api/errors"
               {:query-params {:api_key @_api-key
                               :protocol_version "6"}
                :body (cutil/gzip (.getBytes (che/generate-string exc-map)))}))

(defn post-exc [^Exception e]
  (post (format-exception e)))

(defn post-async [exc-map]
  (send-off (agent exc-map) post))

(defn post-exc-async [^Exception e]
  (post-async (format-exception e)))

(defmacro catch-exc
  "Catch exception, post to Exceptional, return exception."
  [& body]
  `(try
     ~@body
     (catch Exception e# (post-exc e#) e#)))

(defmacro catch-mod
  [mod & body]
  `(try
     ~@body
     (catch Exception e# (do (post-async
                              (-> (format-exception e#)
                                  ~mod))
                             e#))))

(defmacro rethrow-exc
  "Catch exception, post to Exceptional, re-throw."
  [& body]
  `(try
     ~@body
     (catch Exception e# (do (post-exc-async e#)
                             (throw e#)))))

(defmacro rethrow-mod
  [mod & body]
  `(try
     ~@body
     (catch Exception e# (do (post-async
                              (-> (format-exception e#)
                                  ~mod))
                             (throw e#)))))

(defn add-request [exc-map req]
  (request exc-map
           (session (:session req))
           (remote-ip (:remote-addr req))
           (params (or (:params req)
                       (merge (:query-params req)
                              (:form-params req))))
           (url (:uri req))
           (req-method (:request-method req))
           (headers (:headers req))))


(defn wrap-exceptional-catch
  "Ring middleware for posting all exceptions to Exceptional.  Exceptions
   are rethrown after posting. Adds request data to exceptional map."
  [handler]
  (fn [req]
    (catch-mod
     (add-request req)
     (handler req))))

(defn wrap-exceptional-rethrow
  "Ring middleware for posting all exceptions to Exceptional.  Exceptions
   are rethrown after posting. Adds request data to exceptional map."
  [handler]
  (fn [req]
    (rethrow-mod
     (add-request req)
     (handler req))))

(comment

  ;; Examples from README
  
  ;; Please don't spam my account.
  
  (key! "cfaabdee74ffc4d3e7c35391b0079629091f21c9")

  (post-exc (Exception. "first example"))
  (post-exc-async (Exception. "second example"))

  (catch-exc
   (throw (Exception. "third example")))

  (rethrow-exc
   (throw (Exception. "fourth example")))

  (catch-mod
   (add-request {:remote-addr "0:0:0:0:0:0:0:1%0",
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
   (throw (Exception. "fifth example"))))
