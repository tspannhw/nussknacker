package pl.touk.nussknacker.engine.embedded

import pl.touk.nussknacker.engine.api.deployment.simple.SimpleStateStatus
import pl.touk.nussknacker.engine.api.deployment.{OverridingProcessStateDefinitionManager, ProcessActionType}

object EmbeddedProcessStateDefinitionManager extends OverridingProcessStateDefinitionManager(
  statusActionsPF = {
    case SimpleStateStatus.Restarting => List(ProcessActionType.Cancel)
    // We don't know if it is temporal problem or not so deploy is still available
    case  EmbeddedStateStatus.DetailedFailedStateStatus(_) => List(ProcessActionType.Deploy, ProcessActionType.Cancel)
  },
  statusIconsPF = {
    case EmbeddedStateStatus.DetailedFailedStateStatus(_) => "/assets/states/failed.svg"
  },
  statusTooltipsPF = {
    case EmbeddedStateStatus.DetailedFailedStateStatus(message) => s"Problems detected: $message"
  },
  statusDescriptionsPF = {
    case EmbeddedStateStatus.DetailedFailedStateStatus(_) => "There are some problems with scenario."
  }
)
