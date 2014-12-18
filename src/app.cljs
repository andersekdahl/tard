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

;;(defn cljslab-app [{:keys [todos] :as app} owner]
;;  (reify
;;    om/IRender
;;    (render [_]
;;      (html [:div "Hello world!"
;;              [:ul (for [todo todos]
;;                [:li
;;                  {:key (:id todo)}
;;                  (:text todo)])]
;;              [:input {:type "text" :ref "new-todo"}]
;;              (html/submit-button
;;                {:on-click (partial add-todo app owner)}
;;                "React!")]))))


(defn todo-view [todo owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [delete]}]
      (dom/li nil
        (:text todo)
        (dom/button #js {:onClick (fn [e] (put! delete @todo))} "Delete")))))

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
      (dom/div nil
        (dom/h2 nil "Todo list")
        (apply dom/ul nil
          (om/build-all todo-view (:todos app)
              {:init-state {:delete delete}}))
                (dom/div nil
                  (dom/input #js {:type "text" :ref "new-todo"})
                  (dom/button #js {:onClick #(add-todo app owner)} "Add todo")) ))))

(om/root todos-view  app-state
  {:target (.querySelector js/document "body")})
