(ns cljslab.app
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [secretary.macros :refer [defroute]])
  (:require [goog.events :as events]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! <! chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary]
            [clojure.string :as string])
  (:import [goog History]
           [goog.history EventType]))

(def app-state (atom {:todos []}))

(defn cljslab-app [{:keys [todos] :as app} owner]
  (reify
    om/IRender
    (render [this]
      (html [:div "Hello world!"
              [:ul (for [n (range 1 10)]
                [:li n])]
              (html/submit-button "React!")]))))

(om/root cljslab-app app-state
  {:target (.querySelector js/document "body")})