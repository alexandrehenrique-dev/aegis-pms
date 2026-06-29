package br.com.byop.aegis.knowledgegraph.controller;

import br.com.byop.aegis.core.CoreErrorResponse;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphEdgeException;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphNodeException;
import br.com.byop.aegis.knowledgegraph.exception.GraphNodeNotFoundException;
import br.com.byop.aegis.knowledgegraph.exception.InvalidGraphEdgeException;
import br.com.byop.aegis.knowledgegraph.exception.InvalidGraphNodeException;
import br.com.byop.aegis.knowledgegraph.exception.InvalidGraphOrphanActionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = KnowledgeGraphController.class)
public class KnowledgeGraphExceptionHandler {

    @ExceptionHandler(DuplicateGraphNodeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleDuplicateGraphNode() {
        return new CoreErrorResponse("GRAPH_NODE_ALREADY_EXISTS");
    }

    @ExceptionHandler(DuplicateGraphEdgeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleDuplicateGraphEdge() {
        return new CoreErrorResponse("GRAPH_EDGE_ALREADY_EXISTS");
    }

    @ExceptionHandler(GraphNodeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleGraphNodeNotFound() {
        return new CoreErrorResponse("GRAPH_NODE_NOT_FOUND");
    }

    @ExceptionHandler(InvalidGraphNodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidGraphNode() {
        return new CoreErrorResponse("INVALID_GRAPH_NODE");
    }

    @ExceptionHandler(InvalidGraphEdgeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidGraphEdge() {
        return new CoreErrorResponse("INVALID_GRAPH_EDGE");
    }

    @ExceptionHandler(InvalidGraphOrphanActionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidGraphOrphanAction() {
        return new CoreErrorResponse("INVALID_GRAPH_ORPHAN_ACTION");
    }
}
