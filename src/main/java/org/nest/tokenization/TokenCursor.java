package org.nest.tokenization;

import java.util.ArrayDeque;
import java.util.Deque;

/// A cursor for navigating through a TokenList with capabilities for token consumption,
/// lookahead, and backtracking. This class is used in parsing to handle token iteration
/// while allowing for speculative parsing with rollback capability.
public class TokenCursor {
    private final TokenList tokens;
    private final boolean ignoreComments;
    private int currentPosition;
    private final Deque<Integer> savedPositions;
    
    /// Creates a new TokenCursor positioned at the start of the token list.
    ///
    /// @param tokens The TokenList to wrap
    /// @param ignoreComments Whether to skip over Comment tokens automatically
    public TokenCursor(TokenList tokens, boolean ignoreComments) {
        this.tokens = tokens;
        this.ignoreComments = ignoreComments;
        this.currentPosition = 0;
        this.savedPositions = new ArrayDeque<>();
        
        // If ignoreComments is true, skip any comments at the beginning
        if (ignoreComments) {
            skipComments();
        }
    }
    
    /// Creates a new TokenCursor with comment handling defaulted to false.
    ///
    /// @param tokens The TokenList to wrap
    public TokenCursor(TokenList tokens) {
        this(tokens, false);
    }
    
    /// Checks if there are more tokens to consume.
    ///
    /// @return true if there are more tokens available, false otherwise
    public boolean hasMore() {
        return currentPosition < tokens.size();
    }
    
    /// Peeks at the current token without consuming it.
    ///
    /// @return the current token, or null if at the end of the token list
    public Token peek() {
        if (!hasMore()) {
            return null;
        }
        return tokens.get(currentPosition);
    }
    
    /// Peeks at a token ahead of the current position without consuming any tokens.
    ///
    /// @param offset The number of tokens to look ahead (0 for current token)
    /// @return the token at the specified position, or null if beyond the end of the token list
    public Token peek(int offset) {
        int targetPos = currentPosition + offset;
        if (targetPos < 0 || targetPos >= tokens.size()) {
            return null;
        }
        return tokens.get(targetPos);
    }
    
    /// Consumes the current token and advances to the next token.
    /// If ignoreComments is true, continues consuming until a non-comment token is found.
    ///
    /// @return the consumed token, or null if at the end of the token list
    public Token consume() {
        if (!hasMore()) {
            return null;
        }
        
        Token token = tokens.get(currentPosition);
        currentPosition++;
        
        // Skip comments if configured to do so
        if (ignoreComments) {
            skipComments();
        }
        
        return token;
    }
    
    /// Saves the current position for later backtracking.
    /// Multiple positions can be saved in a stack-like manner.
    ///
    /// @return the position that was saved
    public int savePosition() {
        savedPositions.push(currentPosition);
        return currentPosition;
    }
    
    /// Restores the cursor to the most recently saved position.
    ///
    /// @return the position that was restored to
    /// @throws IllegalStateException if no positions have been saved
    public int backtrack() {
        if (savedPositions.isEmpty()) {
            throw new IllegalStateException("Cannot backtrack - no saved positions available");
        }
        
        currentPosition = savedPositions.pop();
        return currentPosition;
    }
    
    /// Discards the most recently saved position without backtracking.
    /// This is typically used when a speculative parse has succeeded and
    /// the saved position is no longer needed.
    public void commitPosition() {
        if (!savedPositions.isEmpty()) {
            savedPositions.pop();
        }
    }
    
    /// Gets the current position in the token list.
    ///
    /// @return the current position index
    public int getPosition() {
        return currentPosition;
    }
    
    /// Sets the cursor to a specific position in the token list.
    ///
    /// @param position the position to move to
    /// @throws IndexOutOfBoundsException if the position is negative or beyond the token list size
    public void setPosition(int position) {
        if (position < 0 || position > tokens.size()) {
            throw new IndexOutOfBoundsException("Invalid position: " + position);
        }
        
        currentPosition = position;
        
        // Skip comments if configured to do so
        if (ignoreComments) {
            skipComments();
        }
    }
    
    /// Checks if the current token matches a specific token type.
    ///
    /// @param tokenClass the class of token to check for
    /// @return true if the current token is of the specified type, false otherwise
    public boolean matches(Class<? extends Token> tokenClass) {
        Token current = peek();
        return current != null && tokenClass.isInstance(current);
    }
    
    /// Advances the cursor past any Comment tokens if ignoreComments is true.
    private void skipComments() {
        while (hasMore() && peek() instanceof Token.Comment) {
            currentPosition++;
        }
    }
    
    /// Returns the remaining number of tokens from the current position.
    ///
    /// @return the number of tokens remaining
    public int remaining() {
        return tokens.size() - currentPosition;
    }
    
    /// Consumes tokens as long as they match the specified token type.
    ///
    /// @param tokenClass the class of token to consume
    /// @return the number of tokens consumed
    public int consumeWhileMatches(Class<? extends Token> tokenClass) {
        int count = 0;
        while (hasMore() && matches(tokenClass)) {
            consume();
            count++;
        }
        return count;
    }
}
