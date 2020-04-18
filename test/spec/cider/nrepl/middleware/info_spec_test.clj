(ns cider.nrepl.middleware.info-spec-test
  (:require
   [cider.nrepl.test-session :as session]
   [clojure.spec.alpha :as s]
   [clojure.test :refer :all]
   [cider.test-ns.first-test-ns]
   [cider.test-ns.second-test-ns]
   [cider.test-ns.third-test-ns]))

(defn ranged-rand
  "Returns random int in range start <= rand < end."
  [start end]
  (+ start (long (rand (- end start)))))

(s/fdef ranged-rand
  :args (s/and (s/cat :start int? :end int?)
               #(< (:start %) (:end %)))
  :ret int?
  :fn (s/and #(>= (:ret %) (-> % :args :start))
             #(< (:ret %) (-> % :args :end))))

(use-fixtures :each session/session-fixture)

(deftest integration-test
  (testing "spec info on a normal function with spec"
    (let [response (session/message {:op "info" :sym "ranged-rand" :ns "cider.nrepl.middleware.info-spec-test"})]
      (is (= (:status response) #{"done"}))
      (is (= (:ns response) "cider.nrepl.middleware.info-spec-test"))
      (is (= (:name response) "ranged-rand"))
      (is (= (:arglists-str response) "[start end]"))
      (is (nil? (:macro response)))
      (is (= (:doc response) "Returns random int in range start <= rand < end."))
      (is (= (:spec response) ["clojure.spec.alpha/fspec"
                               ":args" ["clojure.spec.alpha/and"
                                        ["clojure.spec.alpha/cat" ":start" "clojure.core/int?" ":end" "clojure.core/int?"]
                                        ["clojure.core/fn" ["%"] ["clojure.core/<" [":start" "%"] [":end" "%"]]]]
                               ":ret" "clojure.core/int?"
                               ":fn" ["clojure.spec.alpha/and"
                                      ["clojure.core/fn" ["%"] ["clojure.core/>=" [":ret" "%"] ["clojure.core/->" "%" ":args" ":start"]]]
                                      ["clojure.core/fn" ["%"] ["clojure.core/<" [":ret" "%"] ["clojure.core/->" "%" ":args" ":end"]]]]]))))
  (testing "same name testing function without a spec"
      ;; spec is not defined for this function
    (let [response (session/message {:op "info" :sym "same-name-testing-function" :ns "cider.test-ns.first-test-ns"})]
      (is (= (:status response) #{"done"}))
      (is (= (:ns response) "cider.test-ns.first-test-ns"))
      (is (= (:name response) "same-name-testing-function"))
      (is (= (:arglists-str response) "[]"))
      (is (nil? (:macro response)))
      (is (= (:doc response) "Multiple vars with the same name in different ns's. Used to test ns-list-vars-by-name."))
      (is (nil? (:spec response)))))

  (testing "spec info on clojure.core/let"
    (let [response (session/message {:op "info" :sym "let" :ns "cider.nrepl.middleware.info-spec-test"})]
      (is (= (:status response) #{"done"}))
      (is (= (:ns response) "clojure.core"))
      (is (= (:name response) "let"))
      (is (= (:spec response) ["clojure.spec.alpha/fspec"
                               ":args" ["clojure.spec.alpha/cat"
                                        ":bindings" ":clojure.core.specs.alpha/bindings"
                                        ":body" ["clojure.spec.alpha/*" "clojure.core/any?"]]
                               ":ret" "clojure.core/any?" ":fn" ""])))))
