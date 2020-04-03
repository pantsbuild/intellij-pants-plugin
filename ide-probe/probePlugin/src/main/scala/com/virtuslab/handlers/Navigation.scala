package com.twitter.handlers
import com.intellij.navigation.ChooseByNameContributor
import com.twitter.ideprobe.protocol.NavigationQuery
import com.twitter.ideprobe.protocol.NavigationTarget
import scala.collection.mutable

object Navigation extends IntelliJApi {
  def find(query: NavigationQuery): List[NavigationTarget] = {
    val project = Projects.resolve(query.project)
    val all = mutable.Buffer[NavigationTarget]()

    ChooseByNameContributor.CLASS_EP_NAME.forEachExtensionSafe { contributor =>
      contributor
        .getItemsByName(query.value, "", project, false)
        .map(item => NavigationTarget(item.getName, item.getPresentation.getLocationString))
        .foreach(all.addOne)
    }

    all.toList
  }
}
