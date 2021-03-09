package aqua.parser.ast

import aqua.interim.types.ScalarType
import aqua.parser.lexer.{ArrowTypeToken, BasicTypeToken, CustomTypeToken, DataTypeToken, Name}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import aqua.parser.lift.LiftParser.Implicits._
import cats.Id

class ArrowTypeExprSpec extends AnyFlatSpec with Matchers with EitherValues {
  "arrow type parser" should "parse" in {
    ArrowTypeExpr.p[Id].parseAll("func: A -> u32").right.value should be(
      ArrowTypeExpr[Id](
        Name[Id]("func"),
        ArrowTypeToken[Id](
          (),
          (CustomTypeToken[Id]("A"): DataTypeToken[Id]) :: Nil,
          Some(BasicTypeToken[Id](ScalarType.u32))
        )
      )
    )
  }
}
