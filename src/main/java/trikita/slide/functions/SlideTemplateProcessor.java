package trikita.slide.functions;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import trikita.slide.ImmutablePresentation;
import trikita.slide.Presentation;
import trikita.slide.Slide;

public class SlideTemplateProcessor implements Function<Slide.Builder,Slide.Builder> {
    @Override
    public Slide.Builder apply(Slide.Builder p) {
        return p.withText(p.presentation.templateBefore() + p.text + p.presentation.templateAfter());
    }
}
