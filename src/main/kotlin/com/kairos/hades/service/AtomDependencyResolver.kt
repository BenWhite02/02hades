// =============================================================================
// File: src/main/kotlin/com/kairos/hades/service/AtomDependencyResolver.kt
// 🔥 HADES Atom Dependency Resolver
// Author: Sankhadeep Banerjee
// Service for resolving and managing atom dependencies
// =============================================================================

package com.kairos.hades.service

import com.kairos.hades.atoms.EligibilityAtom
import com.kairos.hades.exception.AtomDependencyException
import com.kairos.hades.exception.AtomNotFoundException
import com.kairos.hades.multitenancy.TenantContext
import com.kairos.hades.repository.EligibilityAtomRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Service for resolving atom dependencies
 * Handles dependency lookup, caching, and validation
 */
@Service
class AtomDependencyResolver @Autowired constructor(
    private val atomRepository: EligibilityAtomRepository,
    private val tenantContext: TenantContext
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(AtomDependencyResolver::class.java)
    }
    
    /**
     * Resolve an atom by its code (cached)
     */
    @Cacheable(value = ["atoms"], key = "#tenantId + ':' + #atomCode")
    fun resolveAtom(atomCode: String, tenantId: String = tenantContext.getTenantId()): EligibilityAtom {
        logger.debug("Resolving atom '{}' for tenant '{}'", atomCode, tenantId)
        
        return atomRepository.findByTenantIdAndCode(tenantId, atomCode)
            ?: throw AtomNotFoundException("Atom not found: $atomCode")
    }
    
    /**
     * Resolve all dependencies for an atom
     */
    fun resolveDependencies(atom: EligibilityAtom): Map<String, EligibilityAtom> {
        if (atom.dependencies.isEmpty()) {
            return emptyMap()
        }
        
        logger.debug("Resolving {} dependencies for atom '{}'", atom.dependencies.size, atom.code)
        
        val resolvedDependencies = mutableMapOf<String, EligibilityAtom>()
        val missingDependencies = mutableListOf<String>()
        
        atom.dependencies.forEach { dependencyCode ->
            try {
                val dependencyAtom = resolveAtom(dependencyCode)
                resolvedDependencies[dependencyCode] = dependencyAtom
            } catch (e: AtomNotFoundException) {
                missingDependencies.add(dependencyCode)
            }
        }
        
        if (missingDependencies.isNotEmpty()) {
            throw AtomDependencyException(
                "Missing dependencies for atom '${atom.code}': ${missingDependencies.joinToString(", ")}",
                missingDependencies
            )
        }
        
        return resolvedDependencies
    }
    
    /**
     * Build dependency graph for an atom
     */
    fun buildDependencyGraph(atom: EligibilityAtom): DependencyGraph {
        return buildDependencyGraph(atom.code, mutableSetOf(), 0)
    }
    
    private fun buildDependencyGraph(
        atomCode: String, 
        visited: MutableSet<String>, 
        depth: Int
    ): DependencyGraph {
        if (depth > 10) { // Prevent infinite recursion
            throw AtomDependencyException("Dependency chain too deep for atom: $atomCode")
        }
        
        if (visited.contains(atomCode)) {
            throw AtomDependencyException("Circular dependency detected: $atomCode")
        }
        
        visited.add(atomCode)
        
        val atom = resolveAtom(atomCode)
        val childGraphs = atom.dependencies.map { dependency ->
            buildDependencyGraph(dependency, visited.toMutableSet(), depth + 1)
        }
        
        visited.remove(atomCode)
        
        return DependencyGraph(
            atomCode = atomCode,
            atom = atom,
            dependencies = childGraphs,
            depth = depth
        )
    }
}

/**
 * Represents a dependency graph for an atom
 */
data class DependencyGraph(
    val atomCode: String,
    val atom: EligibilityAtom,
    val dependencies: List<DependencyGraph>,
    val depth: Int
) {
    /**
     * Get all atoms in the dependency tree (flattened)
     */
    fun getAllAtoms(): Set<EligibilityAtom> {
        val allAtoms = mutableSetOf(atom)
        dependencies.forEach { dependency ->
            allAtoms.addAll(dependency.getAllAtoms())
        }
        return allAtoms
    }
    
    /**
     * Get execution order (dependencies first)
     */
    fun getExecutionOrder(): List<EligibilityAtom> {
        val order = mutableListOf<EligibilityAtom>()
        val visited = mutableSetOf<String>()
        
        fun addToOrder(graph: DependencyGraph) {
            if (!visited.contains(graph.atomCode)) {
                visited.add(graph.atomCode)
                
                // Add dependencies first
                graph.dependencies.forEach { dep ->
                    addToOrder(dep)
                }
                
                // Then add current atom
                order.add(graph.atom)
            }
        }
        
        addToOrder(this)
        return order
    }
}