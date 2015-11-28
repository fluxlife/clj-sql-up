(ns clj-sql-up.migration-test
  (:require [clojure.test :refer :all]
            [clj-sql-up.create :as c]
            [clj-sql-up.migrate :as m]
            [clj-sql-up.migration-files :as mf]
            [leiningen.clj-sql-up :as leinup] ;)
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def db-spec {:subprotocol "hsqldb"
              :subname "mem:testdb"})

(defn- count-records [db table-name]
  (->
    (sql/query db-spec [(str "select count(*) from " table-name)])
    first
    :c1))

;;from https://gist.github.com/edw/5128978
(defn- delete-recursively [fname]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               (io/delete-file f))]
    (func func (io/file fname))))

(deftest test-create-migration
  (testing "create"
    (let [dir "test/clj_sql_up/new_migrations"]
      (try
        (binding [mf/*migration-dir* dir]
          (let [path (c/create ["yo"])]
            (.exists (io/as-file path))))
        (finally (delete-recursively dir))))))

(deftest test-create-migration-dir-with-lein
  (testing "create with lein and migration-dir config"
    (let [config-migration-dir "./test/clj_sql_up/my_path_to/migrations"
          config {:migration-dir config-migration-dir :database db-spec}]
      (try
        (leinup/clj-sql-up config "create" "yo")
        (.isDirectory (io/file config-migration-dir))
        (finally (delete-recursively config-migration-dir)))))

  (testing "create with lein but without migration-dir config"
    (let [default-migration-dir "migrations"]
      (try
        (leinup/clj-sql-up {:database db-spec} "create" "yo")
        (is (= default-migration-dir mf/*migration-dir*))
        (.exists (io/as-file default-migration-dir))
        (finally (delete-recursively mf/*migration-dir*))))))

(deftest test-migrate-and-rollback

  (binding [mf/*migration-dir* "test/clj_sql_up/migrations"]

    (testing "migrate"
      (m/migrate db-spec)

      (let [completed-migrations (m/completed-migrations db-spec)]
        (is (= 4 (count completed-migrations)))
        (is (= 0 (count (m/pending-migrations db-spec))))
        (is (= 0 (count-records db-spec "aaa")))
        (is (= 0 (count-records db-spec "bbb")))
        (is (= 0 (count-records db-spec "ccc")))
        (is (= 0 (count-records db-spec "zzz")))))

    (testing "rollback: 1"
      (m/rollback db-spec 1)
      (let [completed-migrations (m/completed-migrations db-spec)]
        (is (= 3 (count completed-migrations)))
        (is (= 1 (count (m/pending-migrations db-spec))))
        (is (= 0 (count-records db-spec "bbb")))
        (is (= 0 (count-records db-spec "ccc")))
        (is (= 0 (count-records db-spec "zzz")))

        (is (thrown? Exception (count-records db-spec "aaa")))))

    (testing "rollback: 2"

      (m/rollback db-spec 2)
      (let [completed-migrations (m/completed-migrations db-spec)]
        (is (= 1 (count completed-migrations)))
        (is (= 3 (count (m/pending-migrations db-spec))))
        (is (= 0 (count-records db-spec "zzz")))

        (is (thrown? Exception (count-records db-spec "ccc")))
        (is (thrown? Exception (count-records db-spec "bbb")))
        (is (thrown? Exception (count-records db-spec "aaa")))))

    (testing "migrate again"
      (is (= 1 (count (m/completed-migrations db-spec))))

      (m/migrate db-spec)
      (let [completed-migrations (m/completed-migrations db-spec)]
        (is (= 4 (count completed-migrations)))
        (is (= 0 (count (m/pending-migrations db-spec))))
        (is (= 0 (count-records db-spec "aaa")))
        (is (= 0 (count-records db-spec "bbb")))
        (is (= 0 (count-records db-spec "ccc")))
        (is (= 0 (count-records db-spec "zzz")))))))


(deftest test-classpath-migrate-and-rollback

  (binding [mf/*migration-dir* "clj_sql_up/classpath_migrations"]

    (testing "migrate"
      (m/migrate db-spec)

      (let [completed-migrations (m/completed-migrations db-spec)]
        (is (= 4 (count completed-migrations)))
        (is (= 0 (count (m/pending-migrations db-spec))))
        (is (= 0 (count-records db-spec "aaa")))
        (is (= 0 (count-records db-spec "bbb")))
        (is (= 0 (count-records db-spec "ccc")))
        (is (= 0 (count-records db-spec "zzz")))))

    (testing "rollback: 1"
      (m/rollback db-spec 1)
      (let [completed-migrations (m/completed-migrations db-spec)]
        (is (= 3 (count completed-migrations)))
        (is (= 1 (count (m/pending-migrations db-spec))))
        (is (= 0 (count-records db-spec "bbb")))
        (is (= 0 (count-records db-spec "ccc")))
        (is (= 0 (count-records db-spec "zzz")))

        (is (thrown? Exception (count-records db-spec "aaa")))))

    (testing "rollback: 2"

      (m/rollback db-spec 2)
      (let [completed-migrations (m/completed-migrations db-spec)]
        (is (= 1 (count completed-migrations)))
        (is (= 3 (count (m/pending-migrations db-spec))))
        (is (= 0 (count-records db-spec "zzz")))

        (is (thrown? Exception (count-records db-spec "ccc")))
        (is (thrown? Exception (count-records db-spec "bbb")))
        (is (thrown? Exception (count-records db-spec "aaa")))))

    (testing "migrate again"
      (is (= 1 (count (m/completed-migrations db-spec))))

      (m/migrate db-spec)
      (let [completed-migrations (m/completed-migrations db-spec)]
        (is (= 4 (count completed-migrations)))
        (is (= 0 (count (m/pending-migrations db-spec))))
        (is (= 0 (count-records db-spec "aaa")))
        (is (= 0 (count-records db-spec "bbb")))
        (is (= 0 (count-records db-spec "ccc")))
        (is (= 0 (count-records db-spec "zzz")))))))
