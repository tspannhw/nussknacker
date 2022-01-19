package pl.touk.nussknacker.engine.management.periodic

import pl.touk.nussknacker.engine.api.ProcessVersion
import pl.touk.nussknacker.engine.api.deployment.{DeploymentData, ExternalDeploymentId, GraphProcess}
import pl.touk.nussknacker.engine.management.periodic.model.DeploymentWithJarData

import scala.concurrent.Future

class JarManagerStub extends JarManager {

  var deployWithJarFuture: Future[Option[ExternalDeploymentId]] = Future.successful(None)
  var lastDeploymentWithJarData: Option[DeploymentWithJarData] = None

  override def prepareDeploymentWithJar(processVersion: ProcessVersion, graphProcess: GraphProcess): Future[DeploymentWithJarData] = {
    Future.successful(
      model.DeploymentWithJarData(processVersion = processVersion, graphProcess = graphProcess, inputConfigDuringExecutionJson = "", jarFileName = "")
    )
  }

  override def deployWithJar(deploymentWithJarData: DeploymentWithJarData, deploymentData: DeploymentData): Future[Option[ExternalDeploymentId]] = {
    lastDeploymentWithJarData = Some(deploymentWithJarData)
    deployWithJarFuture
  }

  override def deleteJar(jarFileName: String): Future[Unit] = Future.successful(())
}
