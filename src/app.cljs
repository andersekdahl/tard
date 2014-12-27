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

(def app-state (atom {:show-checked false :dragging nil :todos [{:id 1 :text "Do stuff" :checked false} {:id 2 :text "Do more stuff" :checked false} {:id 3 :text "Do less stuff" :checked false}]}))

(defn add-todo [app owner]
  (let [new-field (om/get-node owner "new-todo")]
    (do
      (om/transact! app :todos #(conj % {:id (rand-int 1000) :text (.-value new-field)}))
      (set! (.-value new-field) ""))))

;;Left the println for debugging purpose.
(defn update-todo-checked [todo]
  (do
    (let [updated-todo (assoc todo :checked (not (get :checked todo)))
          todos (:todos @app-state)
          checked (not (:checked todo))]
      (let [updated-todos (vec (map #(if (= (:id %) (:id todo))
                                       (assoc % :checked checked)
                                       %) todos))]
        (swap! app-state assoc :todos updated-todos)))))

(defn show-checked [app owner]
    (let [checked (:show-checked @app)]
      (om/transact! app :show-checked #(not checked))))
   
(defn todo-drop [app e]
  (let [todo (:dragging @app-state)]
    ;; TODO: Insert the todo in the right place, don't just place it last
    (om/transact! app :todos (fn [xs] (conj (vec (remove #(= todo %) xs)) todo)))
    (swap! app-state assoc :dragging nil)))


(defn todo-drag-start
  "Take the todo data and serialize, and attach it to the drag event"
  [dragged-todo e]
  ;; This is only needed to make Firefox show the dragging element,
  ;; we don't get the data later
  (.nativeEvent.dataTransfer.setData e "text/plain" (:id @dragged-todo))
  (swap! app-state assoc :dragging @dragged-todo))

(defn todo-view [todo owner]
  ;;The hidden bool should trigger if a todo should be hidden or not. Works in some cases. Fix me please :)
    (reify
      om/IRenderState
      (render-state [this {:keys [delete]}]
        (html [:li
               {:draggable true
                :on-drag-start (partial todo-drag-start todo)}
               (:text todo) " "
               [:input {:type "button" :on-click #(put! delete @todo) :value "Delete"}]
               [:input {:type "checkbox" :checked (:checked todo) :on-click #(update-todo-checked @todo)}]]))))

(defn search-todo [owner text]
  (om/set-state! owner :search-string text))

(defn filter-todos [show-checked search-string todos]
  (filter #(not= (.indexOf (:text %) search-string) -1)
    (filter (fn [td]
      (if show-checked
       true
       (not (:checked td)))) todos)))

(defn todos-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:delete (chan) :search-string ""})
    om/IWillMount
    (will-mount [_]
      (let [delete (om/get-state owner :delete)]
        (go (loop []
          (let [todo (<! delete)]
            (om/transact! app :todos
              (fn [xs] (vec (remove #(= todo %) xs))))
            (recur))))))
    om/IRenderState
    (render-state [this {:keys [delete search-string]}]
       (html [:div {:on-drop #(todo-drop app %)
                    ;; preventDefault is needed because otherwise we don't get
                    ;; the drop event
                    :on-drag-enter #(.preventDefault %)
                    :on-drag-over #(.preventDefault %)}
              [:h2 "Todo list"]
              [:input {:type "checkbox" :on-click #(show-checked app owner)}]
              [:ul
             
              (let [filtered-todos (filter-todos (:show-checked app) search-string (:todos app))]
                (om/build-all todo-view filtered-todos
                              {:init-state {:delete delete} :key :id}))
                 [:div
                  [:input {:type "text" :ref "new-todo"}]
                  [:input {:type "button" :on-click #(add-todo app owner) :value "Add"}]]
                 [:div
                  [:input 
                    {:type "text" 
                     :ref "search-field"
                     :placeholder "Search" 
                     :on-key-up #(search-todo owner (.-value (om/get-node owner "search-field")))}]]]]))))

(om/root todos-view app-state
  {:target (.querySelector js/document "body")})

