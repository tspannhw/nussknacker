import {ExpressionObj} from "../components/graph/node-modal/editors/expression/types"
import {NodeId} from "./node"

export type EdgeType = {
  type:
    | "FilterFalse"
    | "FilterTrue"
    | "SwitchDefault"
    | "SubprocessOutput"
    | "NextSwitch"
    | string,
  name?: string,
  condition?: ExpressionObj,
}

export type Edge = {
  from: NodeId,
  to: NodeId,
  edgeType?: EdgeType,
}
