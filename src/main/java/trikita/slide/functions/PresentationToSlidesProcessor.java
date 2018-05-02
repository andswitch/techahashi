package trikita.slide.functions;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import trikita.slide.Presentation;
import trikita.slide.Slide;

public class PresentationToSlidesProcessor implements Function<Presentation,List<Slide>> {
    @Override
    public List<Slide> apply(Presentation p) {
        return Arrays.stream(p.pages())
                .map(Slide::new)
                .collect(Collectors.toList());
    }
}
