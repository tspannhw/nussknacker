import * as queryString from "query-string"
import React, {memo} from "react"
import ErrorBoundary from "../components/common/ErrorBoundary"
import {ExternalModule, splitUrl, useExternalLib} from "./ExternalLib"
import {ModuleString, ModuleUrl} from "./ExternalLib/types"

export type DynamicTabData = {
  name: string,
  id: string,
  // expected:
  //  * url of working app - to include in iframe
  //  * url ({module}/{path}@{host}/{remoteEntry}.js) of hosted remoteEntry js file (module federation) with default exported react component - included as component
  url: string,
}

const RemoteTabComponent = ({scope}: {scope: ModuleString}) => {
  const {module: {default: Component}} = useExternalLib(scope)
  return <Component/>
}

const RemoteModuleTab = (props: {url: ModuleUrl}) => {
  const [url, scope] = splitUrl(props.url)
  return (
    <ExternalModule url={url}>
      <ErrorBoundary>
        <RemoteTabComponent scope={scope}/>
      </ErrorBoundary>
    </ExternalModule>
  )
}

const IframeTab = ({tab}: {tab: DynamicTabData}) => (
  <iframe
    src={queryString.stringifyUrl({url: tab?.url, query: {iframe: true}})}
    width="100%"
    height={window.innerHeight}
    frameBorder="0"
  />
)

export const DynamicTab = memo(function DynamicComponent({tab}: {tab: DynamicTabData}): JSX.Element {
  return tab?.url.match(/.js$/) ?
    <RemoteModuleTab url={tab.url}/> :
    <IframeTab tab={tab}/>
})
