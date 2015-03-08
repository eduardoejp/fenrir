(defproject fenrir "0.1.1"
  :description "An alternative class-based object-system for Clojure based on hash-maps and multimethods."
  :url "https://github.com/eduardoejp/fenrir"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :dev-dependencies [[org.clojars.rayne/autodoc "0.8.0-SNAPSHOT"]]
  :autodoc {:name "fenrir"
            :description "An alternative class-based object-system for Clojure based on hash-maps and multimethods."
            :copyright "Copyright 2011 Eduardo Julian"
            :web-src-dir "http://github.com/eduardoejp/fenrir/blob/"
            :web-home "http://eduardoejp.github.com/fenrir/"
            :output-path "autodoc"}
  :main fenrir
	)
