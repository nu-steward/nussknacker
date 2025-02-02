package pl.touk.nussknacker.engine.canonize

import cats.Applicative
import cats.data.{NonEmptyList, ValidatedNel}
import pl.touk.nussknacker.engine.graph.node
import pl.touk.nussknacker.engine.graph.sink.SinkRef
import pl.touk.nussknacker.engine.graph.source.SourceRef

import java.util.UUID

// TODO: this is basically a Writer; we should approach it differently?
private[engine] class MaybeArtificial[A](private val value: A, private val errors: List[ProcessUncanonizationError]) {
  def map[B](f: A => B): MaybeArtificial[B] = new MaybeArtificial(f(value), errors)

  def flatMap[B](f: A => MaybeArtificial[B]): MaybeArtificial[B] = {
    val result = f(value)
    new MaybeArtificial[B](result.value, errors ++ result.errors)
  }

  def toValidNel(implicit extractor: MaybeArtificialExtractor[A]): ValidatedNel[ProcessUncanonizationError, A] = {
    import cats.syntax.validated._
    errors match {
      case h :: hs => NonEmptyList.of(h, hs: _*).invalid[A]
      case _ => extract.validNel
    }
  }

  def extract(implicit extractor: MaybeArtificialExtractor[A]): A = extractor.get(errors, value)
}

private[engine] object MaybeArtificial {
  val DummyObjectNamePrefix = "artificialDummyPrefix"

  private val artificalSourceSinkRef = "dummySourceSink"

  implicit val applicative: Applicative[MaybeArtificial] = new Applicative[MaybeArtificial] {
    override def pure[A](x: A): MaybeArtificial[A] = new MaybeArtificial(x, Nil)

    override def ap[A, B](ff: MaybeArtificial[A => B])(fa: MaybeArtificial[A]): MaybeArtificial[B] = {
      new MaybeArtificial(ff.value(fa.value), fa.errors ++ ff.errors)
    }
  }

  //we need to make sure it's unique to prevent weird errors
  private def generateArtificialName() = s"$DummyObjectNamePrefix-${UUID.randomUUID()}"

  def artificialSink(errors: ProcessUncanonizationError*): MaybeArtificial[node.SubsequentNode] =
    new MaybeArtificial(node.EndingNode(node.Sink(generateArtificialName(), SinkRef(artificalSourceSinkRef, Nil), None)), errors.toList)

  def artificialSource(errors: ProcessUncanonizationError*): MaybeArtificial[node.SourceNode] =
    artificialSink(errors: _*).map(node.SourceNode(node.Source(generateArtificialName(), SourceRef(artificalSourceSinkRef, Nil)), _))
}
