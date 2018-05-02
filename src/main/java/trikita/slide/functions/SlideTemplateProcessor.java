package trikita.slide.functions;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import trikita.slide.ImmutablePresentation;
import trikita.slide.Presentation;

public class SlideTemplateProcessor implements Function<Presentation,Presentation> {
    @Override
    public Presentation apply(Presentation p) {
        String[] finalPar = Arrays.stream(p.pages())
                .map(par -> p.templateBefore() + par + p.templateAfter())
                .collect(Collectors.toList())
                .toArray(new String[]{});
        return ImmutablePresentation
                .copyOf(p)
                .withText(p.joinPages(finalPar));
    }
}
