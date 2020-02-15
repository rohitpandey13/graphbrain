from .nlp import print_tree
from .parser_en import ParserEN
from .parser_de import ParserDE


def create_parser(name, lemmas=False, resolve_corefs=False):
    """Creates and returns a parser (as an instanceof a subclass of Parser)
    for the language specified in the parameter. Throws exception if language
    is not implemented.

    Available parsers:
    'en' -- English

    Keyword argument:
    lemmas -- if True, lemma edges are generated by the parser.
    resolve_corefs -- if True, coreference resolution is performed.
    (default: False)
    """
    if name == 'en':
        return ParserEN(lemmas=lemmas, resolve_corefs=resolve_corefs)
    if name == 'de':
        return ParserDE(lemmas=lemmas, resolve_corefs=resolve_corefs)
    else:
        raise RuntimeError('Unknown parser: {}'.format(name))
