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

(enable-console-print!)

(def app-state (atom {:todos [{:id 1 :text "Do stuff"} {:id 2 :text "Do more stuff"} {:id 3 :text "Do less stuff"}]}))

(defn add-todo [app owner]
  (let [new-field (om/get-node owner "new-todo")]
    (do 
      (om/transact! app :todos #(conj % {:id (rand-int 1000) :text (.-value new-field)}))
      (println @app-state))))

(defn cljslab-app [{:keys [todos] :as app} owner]
  (reify
    om/IRender
    (render [_]
      (html [:div "Hello world!"
              [:ul (for [todo todos]
                [:li 
                  {:key (:id todo)} 
                  (:text todo)])]
              [:input {:type "text" :ref "new-todo"}]
              (html/submit-button 
                {:on-click (partial add-todo app owner)} 
                "React!")]))))

(om/root cljslab-app app-state
  {:target (.querySelector js/document "body")})