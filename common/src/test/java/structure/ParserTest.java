package structure;

import com.dfsek.terra.api.math.vector.Location;
import com.dfsek.terra.api.platform.world.Chunk;
import com.dfsek.terra.api.structures.parser.Parser;
import com.dfsek.terra.api.structures.parser.exceptions.ParseException;
import com.dfsek.terra.api.structures.parser.lang.Block;
import com.dfsek.terra.api.structures.parser.lang.Returnable;
import com.dfsek.terra.api.structures.parser.lang.functions.Function;
import com.dfsek.terra.api.structures.parser.lang.functions.FunctionBuilder;
import com.dfsek.terra.api.structures.tokenizer.Position;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class ParserTest {
    @Test
    public void parse() throws IOException, ParseException {
        Parser parser = new Parser(IOUtils.toString(getClass().getResourceAsStream("/test.tesf")));

        parser.addFunction("test", new FunctionBuilder<Test1>() {
            @Override
            public Test1 build(List<Returnable<?>> argumentList, Position position) throws ParseException {
                return new Test1(argumentList.get(0), argumentList.get(1), position);
            }

            @Override
            public int argNumber() {
                return 2;
            }

            @Override
            public Returnable.ReturnType getArgument(int position) {
                switch(position) {
                    case 0:
                        return Returnable.ReturnType.STRING;
                    case 1:
                        return Returnable.ReturnType.NUMBER;
                    default:
                        return null;
                }
            }

        });

        long l = System.nanoTime();
        Block block = parser.parse();
        long t = System.nanoTime() - l;
        System.out.println("Took " + (double) t / 1000000);

        block.apply(null);

        block.apply(null);
    }

    private static class Test1 implements Function<Void> {
        private final Returnable<?> a;
        private final Returnable<?> b;
        private final Position position;

        public Test1(Returnable<?> a, Returnable<?> b, Position position) {
            this.a = a;
            this.b = b;
            this.position = position;
        }

        @Override
        public Void apply(Location location) {
            System.out.println("string: " + a.apply(location) + ", double: " + b.apply(location));
            return null;
        }

        @Override
        public Void apply(Location location, Chunk chunk) {
            return null;
        }

        @Override
        public Position getPosition() {
            return position;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public ReturnType returnType() {
            return ReturnType.VOID;
        }
    }
}
