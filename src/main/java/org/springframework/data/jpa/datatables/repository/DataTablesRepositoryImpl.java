package org.springframework.data.jpa.datatables.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.datatables.parameter.ColumnParameter;
import org.springframework.data.jpa.datatables.parameter.OrderParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository implementation
 *
 * @author Damien Arrachequesne
 */
public class DataTablesRepositoryImpl<T, ID extends Serializable> extends
		SimpleJpaRepository<T, ID> implements DataTablesRepository<T, ID> {

	public DataTablesRepositoryImpl(
			JpaEntityInformation<T, ?> entityInformation,
			EntityManager entityManager) {

		super(entityInformation, entityManager);
	}

	@Override
	public DataTablesOutput<T> findAll(DataTablesInput input) {
		return findAll(input, null);
	}

	@Override
	public DataTablesOutput<T> findAll(DataTablesInput input,
			Specification<T> additionalSpecification) {
		DataTablesOutput<T> output = new DataTablesOutput<T>();
		output.setDraw(input.getDraw());

		try {
			Page<T> data = findAll(
					Specifications.where(new DataTablesSpecification<T>(input))
							.and(additionalSpecification), getPageable(input));

			output.setData(data.getContent());
			output.setRecordsFiltered(data.getTotalElements());
			output.setRecordsTotal(count());

		} catch (Exception e) {
			output.setError(e.getMessage());
		}

		return output;
	}

	/**
	 * Creates a 'LIMIT .. OFFSET .. ORDER BY ..' clause for the given
	 * {@link DataTablesInput}.
	 *
	 * @param input
	 *            the {@link DataTablesInput} mapped from the Ajax request
	 * @return a {@link Pageable}, must not be {@literal null}.
	 */
    private Pageable getPageable(DataTablesInput input) {
        List<Sort.Order> springOrders = new ArrayList<>();
        List<OrderParameter> orders = input.getOrder();
        for (OrderParameter order : orders) {
            ColumnParameter column = input.getColumns().get(order.getColumn());
            if (column.getOrderable()) {
                String sortColumn = column.getData();
                Sort.Direction sortDirection = Sort.Direction.fromString(order.getDir());
                Sort.Order springOrder = new Sort.Order(sortDirection, sortColumn);
                springOrders.add(springOrder);
            }
        }
        final Sort springSort;
        if (!springOrders.isEmpty()) {
            springSort = new Sort(springOrders);
        } else {
            springSort = null;
        }

        return new PageRequest(input.getStart() / input.getLength(), input.getLength(), springSort);
    }
  }
