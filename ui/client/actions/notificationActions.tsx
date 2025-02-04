import React from "react"
import Notifications from "react-notification-system-redux"
import {v4 as uuid4} from "uuid"
import InlinedSvgs from "../assets/icons/InlinedSvgs"
import Notification from "../components/notifications/Notification"
import {Action} from "./reduxTypes"

export function success(message: string): Action {
  return Notifications.success({
    autoDismiss: 10,
    children: [
      <Notification icon={InlinedSvgs.tipsSuccess} message={message} key={uuid4()}/>,
    ],
  })
}

export function error(message: string, error?: string, showErrorText?: boolean): Action {
  const details = showErrorText && error ? error : null
  return Notifications.error({
    autoDismiss: 10,
    children: [
      <Notification icon={InlinedSvgs.tipsError} message={message} details={details} key={uuid4()}/>,
    ],
  })
}

export function info(message: string): Action {
  return Notifications.info({
    autoDismiss: 10,
    children: [
      <Notification icon={InlinedSvgs.tipsInfo} message={message} key={uuid4()}/>,
    ],
  })
}
