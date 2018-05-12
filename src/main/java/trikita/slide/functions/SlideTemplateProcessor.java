package trikita.slide.functions;

import java.util.function.Function;

import trikita.slide.Slide;

public class SlideTemplateProcessor implements Function<Slide.Builder,Slide.Builder> {
    @Override
    public Slide.Builder apply(Slide.Builder p) {
        return p.withText(p.presentation.templateBefore() + p.text + p.presentation.templateAfter());
    }
}
