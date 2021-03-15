package aqua.ast.algebra.names

import aqua.ast.algebra.types.{ArrowType, Type}
import aqua.ast.algebra.{ReportError, StackInterpreter}
import aqua.parser.lexer.Token
import cats.data.State
import cats.~>
import monocle.Lens
import monocle.macros.GenLens
import monocle.macros.syntax.all._
import cats.syntax.functor._
import cats.syntax.flatMap._

class NamesInterpreter[F[_], X](implicit lens: Lens[X, NamesState[F]], error: ReportError[F, X])
    extends StackInterpreter[F, X, NamesState[F], NamesFrame[F]](GenLens[NamesState[F]](_.stack))
    with (NameOp[F, *] ~> State[X, *]) {

  def readName(name: String): S[Option[Type]] =
    getState.map(_.stack.collectFirst {
      case frame if frame.names.contains(name) => frame.names(name)
    })

  override def apply[A](fa: NameOp[F, A]): State[X, A] =
    (fa match {
      case rn: ReadName[F] =>
        readName(rn.name.value).flatTap {
          case Some(_) => State.pure(())
          case None => report(rn.name, "Undefined name")
        }
      case ra: ReadArrow[F] =>
        readName(ra.name.value).flatMap {
          case Some(t: ArrowType) =>
            State.pure(Option(t))
          case Some(t) =>
            report(ra.name, s"Arrow type expected, got: $t").as(Option.empty[ArrowType])
          case None =>
            report(ra.name, "Undefined name").as(Option.empty[ArrowType])
        }
      case dn: DefineName[F] =>
        readName(dn.name.value).flatMap {
          case Some(_) => report(dn.name, "This name was already defined in the scope").as(false)
          case None =>
            mapStackHead(report(dn.name, "Cannot define a variable in the root scope").as(false))(
              _.focus(_.names).index(dn.name.value).replace(dn.`type`) -> true
            )
        }
        mapStackHeadE(report(dn.name, "Cannot define in the root scope"))(fr =>
          fr.names.get(dn.name.value) match {
            case Some(_) => Left((dn.name, "Variable with this name was already defined", false))
          }
        )
      case bs: BeginScope[F] =>
        beginScope(NamesFrame(bs.token))
      case _: EndScope[F] =>
        endScope
    }).asInstanceOf[State[X, A]]
}

case class NamesState[F[_]](stack: List[NamesFrame[F]])

case class NamesFrame[F[_]](token: Token[F], names: Map[String, Type] = Map.empty)
