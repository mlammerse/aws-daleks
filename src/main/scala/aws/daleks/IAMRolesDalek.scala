package aws.daleks

import rx.lang.scala._
import scala.collection.JavaConverters._
import com.amazonaws.regions.Region
import com.amazonaws.services.identitymanagement.model.Role
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest
import java.util.List
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesRequest
import com.amazonaws.services.identitymanagement.model.DetachRolePolicyRequest
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest
import com.amazonaws.services.identitymanagement.model.GetPolicyRequest
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesRequest
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesForRoleRequest
import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest
import scala.util.Try

case class IAMRolesDalek(implicit region: Region) extends RxDalek[Role] {
  val iam = new AmazonIdentityManagementClient

  override def list(): List[Role] = iam.listRoles().getRoles

  override def exterminate(ar: Role): Unit = {
    detachPolicies(ar)
    val roleName = ar.getRoleName
    iam.deleteRole(new DeleteRoleRequest().withRoleName(roleName))
  }

  def detachPolicies(ar: Role) = {
    detachRolePolicies(ar)
    detachInlinePolicies(ar)
  }

  def detachInlinePolicies(ar: Role): Unit = {
    var roles = iam.listRolePolicies(
      new ListRolePoliciesRequest()
        .withRoleName(ar.getRoleName))
      .getPolicyNames
      .asScala
      .map { pn =>
        iam.getRolePolicy(new GetRolePolicyRequest()
          .withPolicyName(pn)
          .withRoleName(ar.getRoleName))
      }.foreach { rp =>
        iam.deleteRolePolicy(new DeleteRolePolicyRequest()
          .withRoleName(ar.getRoleName)
          .withPolicyName(rp.getPolicyName))
      }
  }

  def detachRolePolicies(ar: Role): Unit = iam.listAttachedRolePolicies(
    new ListAttachedRolePoliciesRequest()
      .withRoleName(ar.getRoleName))
    .getAttachedPolicies
    .asScala
    .foreach { rp =>
      iam.detachRolePolicy(new DetachRolePolicyRequest()
        .withRoleName(ar.getRoleName)
        .withPolicyArn(rp.getPolicyArn))
    }

  override def describe(ar: Role): Map[String, String] =
    Map(("roleName" -> ar.getRoleName),
      ("hasInstanceProfiles" -> hasInstanceProfiles(ar).toString)
      )

  def hasInstanceProfiles(ar: Role) = {
    var roleName = ar.getRoleName
    val tips = Try {
      iam.listInstanceProfilesForRole(new ListInstanceProfilesForRoleRequest().withRoleName(roleName))
    }
    val hasInstanceProfiles = tips.map(! _.getInstanceProfiles.isEmpty ).getOrElse(false)
    hasInstanceProfiles
  }


  override def mercy(ar: Role) =
    IAM.isMercyOnRole(ar.getRoleName) ||
      hasInstanceProfiles(ar) ||
      isDND(ar.getRoleName) ||
      isLOTR(ar.getRoleName)
}