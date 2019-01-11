package org.jetbrains.plugins.scala.lang.psi.impl.expr

import com.intellij.openapi.util.TextRange
import com.intellij.psi.{PsiElement, PsiPolyVariantReference, PsiReference, ResolveResult}
import com.intellij.util.IncorrectOperationException
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScCaseClauses
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScEnumerator.withDesugared
import org.jetbrains.plugins.scala.lang.psi.api.expr._

trait ScEnumeratorImpl extends ScEnumerator {

  override def forStatement: Option[ScForStatementImpl] = this.parentOfType(classOf[ScForStatementImpl])

  override def desugared: Option[ScEnumerator.DesugaredEnumerator] = forStatement flatMap {
    _.desugareEnumerator(this)
  }

  override def getReference: PsiReference = this

  override def getElement: PsiElement = this

  override def getRangeInElement: TextRange = enumeratorToken.getTextRangeInParent

  private def mapDesugaredRef[R](f: PsiPolyVariantReference => R): Option[R] =
    desugared.flatMap { _.callExpr }.map { ref => f(ref) }

  override def resolve(): PsiElement = mapDesugaredRef { _.resolve() }.orNull

  override def getCanonicalText: String = mapDesugaredRef { _.getCanonicalText }.orNull

  override def handleElementRename(newElementName: String): PsiElement =
    throw new IncorrectOperationException("Can not rename for-enumerator")

  override def bindToElement(element: PsiElement): PsiElement =
    mapDesugaredRef { _.bindToElement(element) }.getOrElse { throw new IncorrectOperationException("Enumerator can not be desugared") }

  override def isReferenceTo(element: PsiElement): Boolean =
    mapDesugaredRef { _.isReferenceTo(element) }.getOrElse { false }

  override def isSoft: Boolean = true

  override def multiResolve(incompleteCode: Boolean): Array[ResolveResult] =
    mapDesugaredRef { _.multiResolve(incompleteCode) }.getOrElse { Array.empty[ResolveResult] }
}

object ScEnumeratorImpl {

  class DesugaredEnumeratorImpl(override val analogMethodCall: ScMethodCall, original: ScEnumerator) extends ScEnumerator.DesugaredEnumerator {
    override def callExpr: Option[ScReferenceExpression] =
      Option(analogMethodCall.getInvokedExpr).collect { case refExpr: ScReferenceExpression => refExpr }

    override def content: Option[ScExpression] = {
      analogMethodCall
        .getLastChild
        .getLastChild
        .asInstanceOf[ScBlockExpr]
        .findLastChildByType[ScCaseClauses](ScalaElementType.CASE_CLAUSES)
        .getLastChild
        .lastChild collect { case block: ScBlock => block}
    }

    override def generatorExpr: Option[ScExpression] = original match {
      case gen: ScGenerator =>
        val desugaredMostInnerEnumOfGen = gen
          .nextSiblings
          .collectFirst { case e@withDesugared(analog) if !e.isInstanceOf[ScGenerator] => analog }
          .getOrElse(this)

        desugaredMostInnerEnumOfGen
          .callExpr
          .collect { case ScReferenceExpression.withQualifier(qualifier) => qualifier }

      case _ => None
    }
  }
}