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

(def app-state (atom {:show-checked false :dragging nil :todos [{:id 1 :text "Do stuff" :checked false :editing false :tag ""} {:id 2 :text "Do more stuff" :checked false :editing false :tag "Job related"} {:id 3 :text "Do less stuff" :checked false :editing false :tag ""}]}))

(defn add-todo [app owner]
  (let [new-field (om/get-node owner "new-todo")
        new-tag (om/get-node owner "new-todo-tag")]
    (do
      (om/transact! app :todos #(conj % {:id (rand-int 1000) :text (.-value new-field) :checked false :editing false :tag (.-value new-tag)}))
      (set! (.-value new-field) "")
       (set! (.-value new-tag) ""))))

(defn update-todo-checked [todo]
  (om/update! todo :checked (not (:checked @todo))))

(defn show-checked [app owner]
  (om/update! app :show-checked (not (:show-checked @app))))
   
(defn todo-drop [app e]
  (let [todo (:dragging @app-state)]
    ;; TODO: Insert the todo in the right place, don't just place it last
    (om/transact! app :todos (fn [xs] (conj (vec (remove #(= todo %) xs)) todo)))
    (om/update! app :dragging nil)))


(defn todo-drag-start
  "Take the todo data and serialize, and attach it to the drag event"
  [dragged-todo e]
  ;; This is only needed to make Firefox show the dragging element,
  ;; we don't get the data later
  (.nativeEvent.dataTransfer.setData e "text/plain" (:id @dragged-todo))
  (swap! app-state assoc :dragging @dragged-todo))

(defn edit-todo [todo]
  (om/update! todo :editing (not (:editing @todo)))
  (println app-state))

(defn update-todo [todo text]
  (om/update! todo :text text)
  (om/update! todo :editing false))

(defn todo-view [todo owner]
  ;;The hidden bool should trigger if a todo should be hidden or not. Works in some cases. Fix me please :)
    (reify
      om/IRenderState
      (render-state [this {:keys [delete]}]
        (html [:li
               {:draggable true
                :on-drag-start (partial todo-drag-start todo)}
               (if (= (:editing todo) false)
                [:span 
                  (:text todo)
                  [:input {:type "button" :on-click #(edit-todo todo) :value "Edit"}]]
                [:span
                  [:input {:type "text" :defaultValue (:text todo) :ref "edit-field"}]
                  [:input {:type "button" :on-click #(update-todo todo (.-value (om/get-node owner "edit-field"))) :value "Done"}]]) 
               " "
               [:input {:type "button" :on-click #(put! delete @todo) :value "Delete"}]
               [:input {:type "checkbox" :checked (:checked todo) :on-click #(update-todo-checked todo)}]
               [:text (:tag todo)]]))))

(defn search-todo [owner text]
  (om/set-state! owner :search-string text))

(defn todo-matches-search? [todo search-string]
  (not= (.indexOf (:text todo) search-string) -1))

(defn filter-todos [show-checked search-string todos]
<<<<<<< HEAD
  (filter #(or (not=(.indexOf (:text %) search-string) -1) (not= (.indexOf (:tag %) search-string) -1))
    (filter (fn [td]
      (if show-checked
       true
       (not (:checked td)))) todos)))
=======
  (filter (fn [td]
            (if show-checked
             (todo-matches-search? td search-string)
             (and (not (:checked td)) (todo-matches-search? td search-string)))) todos))
>>>>>>> a297b70107b1185039caefbfeccd6b2136819bbc

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
                  [:input {:type "text" :ref "new-todo-tag"}]
                  [:input {:type "button" :on-click #(add-todo app owner) :value "Add"}]]
                 [:div
                  [:input 
                    {:type "text" 
                     :ref "search-field"
                     :placeholder "Search" 
                     :on-key-up #(search-todo owner (.-value (om/get-node owner "search-field")))}]]]]))))

(om/root todos-view app-state
  {:target (.querySelector js/document "body")})

