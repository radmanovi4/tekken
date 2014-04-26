(ns tekken.core
    (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require  [tekken.util :as util]
               [om.core :as om :include-macros true]
               [om.dom :as dom :include-macros true]
               [cljs.core.async :refer [put! <! >! chan map>]]))

(defonce data
  (atom {}))

(defn editor
  [app owner {:keys [ch]}]
  (reify
    om/IInitState
    (init-state
      [_]
      {:text "
Напишете някви лайна?

```
(def a 10)
```

- a
- b
+ c
- d

---
"
       :onChange
       (fn [_]
         (let [value (->> (om/get-node owner "text")
                          (.-value))]
           (om/set-state! owner :text value)
           (put! ch value)))})

    om/IDidMount
    (did-mount
     [_]
     ((om/get-state owner :onChange)))

    om/IRenderState
    (render-state
      [_ {:keys [text onChange]}]
      (dom/form
        #js {:action "javascript: void(0);"
             :id "editor"}
        (dom/textarea
          #js {:type "text"
               :ref "text"
               :rows "20"
               :cols "30"
               :value text
               :onChange onChange})))))

(defn viewer
  "Test viewer component."
  [app owner {:keys [ch]}]
  (reify
    om/IInitState
    (init-state
      [_]
     {:markdown
      ""

      :onClick
      (fn [_]
        (let [c (util/html->canvas (om/get-node owner "viewer"))]
          (go (->> (<! c)
                   (util/canvas->pdf)
                   (aset js/window "location")))))
      :onMouseEnter
      (fn [_]
        (let [c (util/html->canvas (om/get-node owner "viewer"))]
          (go (let [cvs (<! c)
                    node (om/get-node owner "preview")]
                (aset node "innerHTML" "")
                (.appendChild node cvs)))))

      :onMouseLeave
      (fn [_]
        (-> (om/get-node owner "preview")
            (aset "innerHTML" "")))})

    om/IWillMount
    (will-mount
      [_]
     (go-loop
      []
      (let [v (<! ch)]
        (om/set-state! owner :markdown v)
        (recur))))

    om/IRenderState
    (render-state
      [_ {:keys [markdown onClick onMouseEnter onMouseLeave]}]
     (dom/div
      nil
      (dom/section
       (clj->js {:id "viewer"
                 :ref "viewer"
                 :dangerouslySetInnerHTML
                 {:__html (util/md->html markdown)}}))
      (dom/a #js {:href "javascript:void(0);"
                  :onClick onClick
                  :onMouseEnter onMouseEnter
                  :onMouseLeave onMouseLeave} "show img")
      (dom/div #js {:ref "preview"})))))

(defn home-ui
  "Home page ui."
  [app owner]
  (reify
    om/IInitState
    (init-state
     [_]
     {:ch (chan)})

    om/IRenderState
    (render-state
     [_ {:keys [ch]}]
     (dom/section
      #js {:className "home"}
      (om/build editor app {:opts {:ch ch}})
      (om/build viewer app {:opts {:ch ch}})))))

(defn tekken
  "The big boss that builds all the components together."
  []
  (om/root
    (fn [app owner]
      (dom/div
        nil
        (om/build home-ui app)))
    data
    {:target (. js/document (getElementById "main"))}))

(tekken)
