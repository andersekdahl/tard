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

(defn todo-view [todo owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [delete]}]
      (html [:li (:text todo) " "
        [:input {:type "button" :on-click #(put! delete @todo) :value "Delete"}]]))))

(defn todos-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:delete (chan)})
    om/IWillMount
    (will-mount [_]
      (let [delete (om/get-state owner :delete)]
        (go (loop []
          (let [todo (<! delete)]
            (om/transact! app :todos
              (fn [xs] (vec (remove #(= todo %) xs))))
            (recur))))))
    om/IRenderState
    (render-state [this {:keys [delete]}]
      (html [:div
              [:h2 "Todo list"]
              [:ul
                (om/build-all todo-view (:todos app)
                  {:init-state {:delete delete}})]
              [:div
                [:input {:type "text" :ref "new-todo"}]
                [:input {:type "button" :on-click #(add-todo app owner) :value "Add"}]]]))))

(om/root todos-view  app-state
  {:target (.querySelector js/document "body")})
