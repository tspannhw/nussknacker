import {g} from "jointjs"
import {mapValues} from "lodash"
import {DropTarget} from "react-dnd"
import {connect} from "react-redux"
import {compose} from "redux"
import {
  getFetchedProcessDetails,
  getLayout,
  getProcessCounts,
  getProcessToDisplay,
} from "../../reducers/selectors/graph"
import {setLinksHovered} from "./dragHelpers"
import {Graph} from "./Graph"
import GraphWrapped from "./GraphWrapped"
import {RECT_HEIGHT, RECT_WIDTH} from "./EspNode/esp"
import NodeUtils from "./NodeUtils"
import {DndTypes} from "../toolbars/creator/Tool"
import {
  injectNode,
  layoutChanged,
  nodeAdded,
  nodesConnected,
  nodesDisconnected,
  resetSelection,
  toggleSelection,
} from "../../actions/nk"

const spec = {
  drop: (props, monitor, component: Graph) => {
    const clientOffset = monitor.getClientOffset()
    const relOffset = component.processGraphPaper.clientToLocalPoint(clientOffset)
    // to make node horizontally aligned
    const nodeInputRelOffset = relOffset.offset(RECT_WIDTH * -.8, RECT_HEIGHT * -.5)
    component.addNode(monitor.getItem(), mapValues(nodeInputRelOffset, Math.round))
    setLinksHovered(component.graph)
  },
  hover: (props, monitor, component: Graph) => {
    const node = monitor.getItem()
    const canInjectNode = NodeUtils.hasInputs(node) && NodeUtils.hasOutputs(node)

    if (canInjectNode) {
      const clientOffset = monitor.getClientOffset()
      const point = component.processGraphPaper.clientToLocalPoint(clientOffset)
      const rect = new g.Rect({...point, width: 0, height: 0})
        .inflate(RECT_WIDTH / 2, RECT_HEIGHT / 2)
        .offset(RECT_WIDTH / 2, RECT_HEIGHT / 2)
        .offset(RECT_WIDTH * -.8, RECT_HEIGHT * -.5)
      setLinksHovered(component.graph, rect)
    } else {
      setLinksHovered(component.graph)
    }
  },
}

const mapState = state => ({
  // eslint-disable-next-line i18next/no-literal-string
  divId: "nk-graph-main",
  nodeSelectionEnabled: true,
  readonly: false,
  processToDisplay: getProcessToDisplay(state),
  fetchedProcessDetails: getFetchedProcessDetails(state),
  processCounts: getProcessCounts(state),
  layout: getLayout(state),
})

export const ProcessGraph = compose(
  DropTarget(DndTypes.ELEMENT, spec, (connect, monitor) => ({
    connectDropTarget: connect.dropTarget(),
    isDraggingOver: monitor.isOver(),
  })),
  //withRef is here so that parent can access methods in graph
  connect(mapState, {
    nodesConnected,
    nodesDisconnected,
    layoutChanged,
    injectNode,
    nodeAdded,
    resetSelection,
    toggleSelection,
  }, null, {forwardRef: true}),
)(GraphWrapped)
