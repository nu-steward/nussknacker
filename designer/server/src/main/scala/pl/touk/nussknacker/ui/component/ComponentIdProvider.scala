package pl.touk.nussknacker.ui.component

import com.typesafe.scalalogging.LazyLogging
import pl.touk.nussknacker.engine.ProcessingTypeData
import pl.touk.nussknacker.engine.api.component.ComponentType.ComponentType
import pl.touk.nussknacker.engine.api.component.{ComponentId, ComponentType}
import pl.touk.nussknacker.engine.component.ComponentUtil
import pl.touk.nussknacker.engine.component.ComponentsUiConfigExtractor.ComponentsUiConfig
import pl.touk.nussknacker.engine.graph.node.{NodeData, WithComponent}
import pl.touk.nussknacker.restmodel.process.ProcessingType
import pl.touk.nussknacker.ui.process.ProcessCategoryService

//TODO: It is work around for components duplication across multiple scenario types, until we figure how to do deduplication.
trait ComponentIdProvider {
  def createComponentId(processingType: ProcessingType, name: Option[String], componentType: ComponentType): ComponentId
  def nodeToComponentId(processingType: ProcessingType, node: NodeData): Option[ComponentId]
}

object DefaultComponentIdProvider extends LazyLogging {

  def createUnsafe(processingTypeDataMap: Map[ProcessingType, ProcessingTypeData],
                   categoryService: ProcessCategoryService): ComponentIdProvider = {
    logger.debug("Creating component id provider")

    val componentObjectsService = new ComponentObjectsService(categoryService)
    val componentObjectsMap = processingTypeDataMap.transform(componentObjectsService.prepareWithoutFragments)
    val componentIdProvider = new DefaultComponentIdProvider(componentObjectsMap.transform { case (_, componentsObjects) => componentsObjects.config })

    ComponentsValidator.checkUnsafe(componentObjectsMap, componentIdProvider)

    componentIdProvider
  }

}

class DefaultComponentIdProvider(configs: Map[ProcessingType, ComponentsUiConfig]) extends ComponentIdProvider {

  override def createComponentId(processingType: ProcessingType, name: Option[ProcessingType], componentType: ComponentType): ComponentId = {
    name match {
      case Some(value) => createComponentId(processingType, value, componentType)
      case None => ComponentId.forBaseComponent(componentType)
    }
  }

  override def nodeToComponentId(processingType: ProcessingType, node: NodeData): Option[ComponentId] =
    ComponentUtil
      .extractComponentType(node)
      .map(componentType => node match {
        case n: WithComponent => createComponentId(processingType, n.componentId, componentType)
        case _ => ComponentId.forBaseComponent(componentType)
      })

  private def createComponentId(processingType: ProcessingType, name: String, componentType: ComponentType): ComponentId = {
    val defaultComponentId = ComponentId.default(processingType, name, componentType)
    val overriddenComponentId = getOverriddenComponentId(processingType, name, defaultComponentId)

    //We assume that base and currently fragment component's id can't be overridden
    if (defaultComponentId != overriddenComponentId && (ComponentType.isBaseComponent(componentType) || componentType == ComponentType.Fragments)) {
      throw new IllegalArgumentException(s"Component id can't be overridden for: '$name' with component type: '$componentType'.")
    }

    overriddenComponentId
  }

  private def getOverriddenComponentId(processingType: ProcessingType, componentName: String, defaultComponentId: ComponentId): ComponentId = {
    def getComponentId(name: String): Option[ComponentId] = configs.get(processingType).flatMap(_.get(name)).flatMap(_.componentId)

    val componentId = getComponentId(componentName)

    //It's work around for components with the same name and different componentType, eg. kafka-avro
    //where default id is combination of processingType-componentType-name
    val componentIdForDefaultComponentId = getComponentId(defaultComponentId.value)

    componentId
      .orElse(componentIdForDefaultComponentId)
      .getOrElse(defaultComponentId)
  }
}
